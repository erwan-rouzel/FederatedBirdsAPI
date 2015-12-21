package fr.ecp.sio.appenginedemo.api;

import fr.ecp.sio.appenginedemo.data.MessagesRepository;
import fr.ecp.sio.appenginedemo.model.Message;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * A servlet to handle all the requests on a specific message
 * All requests with path matching "/messages/*" where * is the id of the message are handled here.
 */
public class MessageServlet extends JsonServlet {

    /**
     * @api {get} /message/:id Get Message
     * @apiName GetMessage
     * @apiGroup Message
     *
     * @apiSuccess {String} id ID of the message.
     * @apiSuccess {String} text Text of the message.
     * @apiSuccess {String} date Date of the message.
     * @apiSuccess {User} user User of the message.
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
     * @apiError messageNotFound The message with ID {id} does not exist
     *
     * @apiErrorExample Error-Response:
     *     HTTP/1.1 404 Not Found
     *     {
     *          "status": 404,
     *          "code": "messageNotFound",
     *          "message": "The message with ID 123 does not exist"
     *     }
     */
    // A GET request should simply return the message
    @Override
    protected Message doGet(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // (OK) TODO: Extract the id of the message from the last part of the path of the request
        // (OK) TODO: Check if this id is syntactically correct
        // (OK) TODO: Not found?
        Long messageId = getLongParameter(req, "id");
        Message message = MessagesRepository.getMessage(
                messageId
        );

        // The message with this ID does not exist in repository
        if(message == null) {
            throw new ApiException(404, "messageNotFound", "The message with ID " + messageId + " does not exist");
        }

        // We don't check rights here as we assume all messages are public

        return message;
    }

    /**
     * @api {post} /message/:id Update Message
     * @apiName PostMessage
     * @apiGroup Message
     *
     * @apiParam (Header parameter) {String} Authorization The token for user in form of "Bearer {token}"
     * @apiParam (Body parameter) {String} JSON representation of the message like {"text": "New value for text..."}
     *
     * @apiSuccess {String} id ID of the message.
     * @apiSuccess {String} text Text of the message.
     * @apiSuccess {String} date Date of the message.
     * @apiSuccess {User} user User of the message.
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
     * @apiError unauthorizedOperation You cannot edit a message which is not yours
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
    // A POST request could be made to modify some properties of a message after it is created
    @Override
    protected Message doPost(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // (OK) TODO: Get the message as below
        // (OK) TODO: Apply the changes
        // (OK) TODO: Return the modified message
        Message messageModified = getJsonRequestBody(req, Message.class);

        // We check that the message is owned by the authenticated user
        if(messageModified.user.get().id != getAuthenticatedUser(req).id) {
            throw new ApiException(400, "unauthorizedOperation", "You cannot edit a message which is not yours");
        }

        // The insert method either adds the new message or modify it if existing
        MessagesRepository.insertMessage(messageModified);

        return messageModified;
    }

    /**
     * @api {delete} /message/:id Delete Message
     * @apiName DeleteMessage
     * @apiGroup Message
     *
     * @apiParam (Header parameter) {String} Authorization The token for user in form of "Bearer {token}"
     * @apiParama (Url parameter) {Number} The ID of the message to delete
     *
     * @apiSuccess {Void} null null
     *
     * @apiSuccessExample Success-Response:
     *     HTTP/1.1 200 OK
     *
     * @apiError unauthorizedOperation You cannot edit a message which is not yours
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
    // A DELETE request should delete a message (if the user)
    @Override
    protected Void doDelete(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // (OK) TODO: Get the message
        // (OK) TODO: Check that the calling user is the author of the message (security!)
        // (OK) TODO: Delete the message
        // A DELETE request shall not have a response body
        Message message = MessagesRepository.getMessage(
                getLongParameter(req, "id")
        );

        if(message.user.get().id != getAuthenticatedUser(req).id) {
            throw new ApiException(400, "unauthorizedOperation", "You cannot delete a message which is not yours");
        }

        MessagesRepository.deleteMessage(message.id);
        return null;
    }

}
