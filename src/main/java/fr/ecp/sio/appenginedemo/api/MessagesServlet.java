package fr.ecp.sio.appenginedemo.api;

import com.googlecode.objectify.Ref;
import fr.ecp.sio.appenginedemo.data.MessagesRepository;
import fr.ecp.sio.appenginedemo.data.UsersRepository;
import fr.ecp.sio.appenginedemo.model.Message;
import fr.ecp.sio.appenginedemo.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A servlet to handle all the requests on a list of messages
 * All requests on the exact path "/messages" are handled here.
 */
public class MessagesServlet extends JsonServlet {

    /**
     * @api {get} /messages List of messages
     * @apiName GetMessages
     * @apiGroup Message
     *
     * @apiParam (Url parameter) {Number} [user] Retrieve the messages of given user.
     *
     * @apiSuccess {Array} messages List of messages that the user can see.
     *
     * @apiError invalidAuthorization Invalid authorization header format
     * @apiError invalidAuthorization Invalid token
     * @apiError userNotFound The user you requested does not exist
     * @apiError unauthorizedMessages You can see only your messages or the messages of followed users
     *
     * @apiSuccessExample Success-Response:
     *     HTTP/1.1 200 OK
     */
    // A GET request should return a list of messages
    @Override
    protected List<Message> doGet(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // (OK) TODO: filter the messages that the user can see (security!)
        // TODO: filter the list based on some parameters (order, limit, scope...)
        // (OK) TODO: e.g. add a parameter to get the messages of a user given its id (i.e. /messages?user=256439)

        User requestedUser;
        User authUser = getAuthenticatedUser(req);
        List<Message> allMessages = MessagesRepository.getMessages();
        List<Message> filteredMessages = new ArrayList<>();

        // We assume that a user can see his own messages only or the messages of the followed users.
        //
        // For performance and memory consumption reasons, this kind of filtering
        // should be refactored later on to be implemented in the data layer.
        //
        // As of now we are not supposed to modify the data layer so we implemented
        // at servlet level by going through all the user messages.

        if(hasParameter(req, "user")) {
            requestedUser = UsersRepository.getUser(getLongParameter(req, "user"));

            if(requestedUser == null) {
                throw new ApiException(400, "userNotFound", "The user you requested does not exist");
            }

            // We check if the requested user is followed by the authenticated user
            boolean isFollowedByAuthUser = false;
            for(User userFollowed: UsersRepository.getUserFollowed(authUser.id, null, null).users) {
                if(userFollowed.id == requestedUser.id) {
                    isFollowedByAuthUser = true;
                }
            }

            if(! isFollowedByAuthUser) {
                throw new ApiException(401, "unauthorizedMessages", "You can see only your messages or the messages of followed users");
            }
        } else {
            requestedUser = authUser;
        }

        for (Message message : allMessages) {
            if(message.user.get().id == requestedUser.id) {
                filteredMessages.add(message);
            }
        }

        return filteredMessages;
    }

    /**
     * @api {post} /messages Post Message
     * @apiName PostMessages
     * @apiGroup Message
     *
     * @apiParam (Header parameter) {String} Authorization The token for user in form of "Bearer {token}"
     * @apiParam (Body parameter) {String} - JSON representation of the message in form {"text": "My message..."}
     *
     * @apiSuccess {String} id ID of the created message.
     * @apiSuccess {String} text Text of the created message.
     * @apiSuccess {String} date Date of the created message.
     *
     * @apiSuccessExample Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *          "id": 5629499534213120,
     *          "text": "My new message...",
     *          "date": "2015-12-16T14:29:06Z",
     *          "user": {
     *              "id": 1,
     *              "login": "user1",
     *              "avatar": "http://storage.googleapis.com/federatedbirds-storage/avatar-1.jpg",
     *              "coverPicture": "",
     *              "email": "user1@yopmail.com",
     *              "password": "0b14d501a594442a01c6859541bcb3e8164d183d32937b851835442f69d5c94e"
     *          }
     *     }
     *
     * @apiError invalidAuthorization Invalid authorization header format
     * @apiError invalidAuthorization Invalid token
     *
     * @apiErrorExample Error-Response:
     *     HTTP/1.1 401 Unauthorized
     *     {
     *          "status": 401,
     *          "code": "invalidAuthorization",
     *          "message": "Invalid authorization header format"
     *     }
     */
    // A POST request on a collection endpoint should create an entry and return it
    @Override
    protected Message doPost(HttpServletRequest req) throws ServletException, IOException, ApiException {

        // The request should be a JSON object describing a new message
        Message message = getJsonRequestBody(req, Message.class);
        if (message == null) {
            throw new ApiException(400, "invalidRequest", "Invalid JSON body");
        }

        // TODO: validate the message here (minimum length, etc.)

        // Some values of the Message should not be sent from the client app
        // Instead, we give them here explicit value
        message.user = Ref.create(getAuthenticatedUser(req));
        message.date = new Date();
        message.id = null;

        // Our message is now ready to be persisted into our repository
        // After this call, our repository should have given it a non-null id
        MessagesRepository.insertMessage(message);

        return message;
    }

}
