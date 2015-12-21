package fr.ecp.sio.appenginedemo.api;

import fr.ecp.sio.appenginedemo.data.DataException;
import fr.ecp.sio.appenginedemo.data.ImagesRepository;
import fr.ecp.sio.appenginedemo.data.MessagesRepository;
import fr.ecp.sio.appenginedemo.data.UsersRepository;
import fr.ecp.sio.appenginedemo.model.Avatar;
import fr.ecp.sio.appenginedemo.model.Message;
import fr.ecp.sio.appenginedemo.model.User;
import fr.ecp.sio.appenginedemo.utils.ValidationUtils;
import org.apache.tika.mime.MimeTypeException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * A servlet to handle all the requests on a specific user
 * All requests with path matching "/users/*" where * is the id of the user are handled here.
 */
public class UserServlet extends JsonServlet {
    /**
     * @api {get} /user/:id Request User information
     * @apiName GetUser
     * @apiGroup User
     *
     * @apiParam (Header parameter) {String} Authorization The token for user in form of "Bearer {token}"
     * @apiParam (Url parameter) {Number} id Users unique ID. This ID can be "me" for currently authenticated user.
     *
     * @apiSuccess {String} login Login of the User.
     * @apiSuccess {String} avatar Avatar of the User.
     * @apiSuccess {String} coverPicture Cover picture of the User.
     * @apiSuccess {String} email Email of the User (hidden if is different from authenticated user)
     * @apiSuccess {String} password Password hash of the user
     *
     * @apiSuccessExample Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *          "id": 6,
     *           "login": "user2",
     *           "avatar": "http://www.gravatar.com/avatar/a2fbe04611692ba9b7a5e148786419d7?d=wavatar",
     *           "coverPicture": "",
     *           "email": "*",
     *           "password": "598a1a400c1dfdf36974e69d7e1bc98593f2e15015eed8e9b7e47a83b31693d5"
     *     }
     *
     * @apiError userNotFound The id of the User was not found.
     * @apiError missingIdParameter You must specify an id parameter.
     *
     * @apiErrorExample Error-Response:
     *     HTTP/1.1 404 Not Found
     *     {
     *          "status": 400,
     *          "code": "userNotFound",
     *          "message": "The user you requested does not exist"
     *     }
     */
    @Override
    protected User doGet(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // (OK) TODO: Extract the id of the user from the last part of the path of the request
        // (OK) TODO: Check if this id is syntactically correct
        // (OK) TODO: Not found?
        // (OK) TODO: Add some mechanism to hide private info about a user (email) except if he is the caller

        // We know already that the userId is numeric and not null
        // because we used the rewrite rule with regex [0-9]+
        // => So we just have to check that the user actually exists
        //
        // We consider that all users are public and are readable by any other users.
        // In case there was specific user rights visibility this should be checked here also.
        //
        // As of now we hide email which is considered as private.

        User user = UsersRepository.getUser(getIdParameter(req));

        if(user == null) {
            throw new ApiException(400, "userNotFound", "The user you requested does not exist");
        }

        // In all cases we never return the user password hash
        user.password = "*";

        // Only if we are not requesting our own information, we hide the email
        if (user.id != getAuthenticatedUser(req).id) {
            user.email = "*";
        }

        return user;
    }

    /**
     * @api {post} /user/:id Update User information
     * @apiName PostUser
     * @apiGroup User
     *
     * @apiParam (Header parameter) {String} Authorization The token for user in form of "Bearer {token}"
     * @apiParam (Url parameter) {Number} id Users unique ID. This ID can be "me" for currently authenticated user.
     * @apiParam (Body parameter) {String} - Json representation of the user. Not all the attributes have to defined but you can define only the attributes which are to be updated.
     * @apiParam (Url query parameter) {Boolean} [followed] Boolean which tells if we are following the user (true|false)
     *
     * @apiSuccess {String} login Login of the User.
     * @apiSuccess {String} avatar Avatar of the User.
     * @apiSuccess {String} coverPicture Cover picture of the User.
     * @apiSuccess {String} email Email of the User (hidden if is different from authenticated user)
     * @apiSuccess {String} password Password hash of the user
     *
     * @apiSuccessExample Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *          "id": 6,
     *           "login": "user2",
     *           "avatar": "http://www.gravatar.com/avatar/a2fbe04611692ba9b7a5e148786419d7?d=wavatar",
     *           "coverPicture": "",
     *           "email": "user2@yopmail.com",
     *           "password": "598a1a400c1dfdf36974e69d7e1bc98593f2e15015eed8e9b7e47a83b31693d5"
     *     }
     *
     * @apiError invalidAuthorization Invalid authorization header format
     * @apiError invalidAuthorization Invalid token
     * @apiError userNotFound The id of the User was not found.
     * @apiError unauthorizedOperation You cannot edit another user than yourself.
     * @apiError invalidLogin Login did not match the specs
     * @apiError invalidPassword Password did not match the specs
     * @apiError duplicateLogin Duplicate login
     * @apiError duplicateEmail Login Duplicate email
     * @apiError invalidAvatar Invalid avatar image
     * @apiError invalidCoverPicture Invalid cover picture image
     *
     * @apiErrorExample Error-Response:
     *     HTTP/1.1 404 Not Found
     *     {
     *          "status": 404,
     *          "code": "userNotFound",
     *          "message": "The user you requested does not exist"
     *     }
     *
     * @apiErrorExample Error-Response:
     *     HTTP/1.1 401 Unauthorized
     *     {
     *          "status": 401,
     *          "code": "unauthorizedOperation",
     *          "message": "You cannot edit another user than yourself"
     *     }
     */
    @Override
    protected User doPost(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // (OK) TODO: Get the user as below
        // (OK) TODO: Apply some changes on the user (after checking for the connected user)
        // (OK) TODO: Handle special parameters like "followed=true" to create or destroy relationships
        // (OK) TODO: Return the modified user

        User userModified = getJsonRequestBody(req, User.class);
        User authUser = getAuthenticatedUser(req);
        long requestedId = getIdParameter(req);

        if(userModified != null) {
            if(requestedId != authUser.id) {
                throw new ApiException(400, "unauthorizedOperation", "You cannot edit another user than yourself");
            }

            // Whether the id of user has been defined in Json or not does not matter
            // => In all cases we force update for user id given as parameter
            userModified.id = authUser.id;

            // Here we allow the Json to define or not any attribute
            // Only the attributes which are actually defined are checked for validity and if valid are updated

            if(userModified.login != null) {
                if (!ValidationUtils.validateLogin(userModified.login)) {
                    throw new ApiException(400, "invalidLogin", "Login did not match the specs");
                }
                if (userModified.login != authUser.login && UsersRepository.getUserByLogin(userModified.login) != null) {
                    throw new ApiException(400, "duplicateLogin", "Duplicate login");
                }

                authUser.login = userModified.login;
            }

            if(userModified.password != null) {
                if (!ValidationUtils.validatePassword(userModified.password)) {
                    throw new ApiException(400, "invalidPassword", "Password did not match the specs");
                }

                authUser.password = generateUserPassword(userModified);
            }

            if(userModified.email != null) {
                if (!ValidationUtils.validateEmail(userModified.email)) {
                    throw new ApiException(400, "invalidEmail", "Invalid email");
                }

                if (!userModified.email.equals(authUser.email) && UsersRepository.getUserByEmail(userModified.email) != null) {
                    throw new ApiException(400, "duplicateEmail", "Duplicate email " + userModified.email + " / " + authUser.email);
                }

                authUser.email = userModified.email;
            }

            if(userModified.avatar != null) {
                if (!ValidationUtils.validateImageUrl(userModified.avatar)) {
                    throw new ApiException(400, "invalidAvatar", "Invalid avatar image");
                }

                authUser.avatar = userModified.avatar;
            }

            if (userModified.coverPicture != null) {
                if (!ValidationUtils.validateImageUrl(userModified.coverPicture)) {
                    throw new ApiException(400, "invalidCoverPicture", "Invalid cover picture image");
                }

                authUser.coverPicture = userModified.coverPicture;
            }

            // Finally we save the user which has received all valid updated attributes from userModified
            UsersRepository.saveUser(authUser);
        }

        // Follow or unfollow a user. We are here if call was made on /user/{id}/followed
        // which has called the rewriterule putting the id of user as a parameter
        if(hasParameter(req, "followed")) {
            UsersRepository.setUserFollowed(
                    authUser.id,
                    requestedId,
                    getBooleanParameter(req, "followed")
            );
        }

        return authUser;
    }

    /**
     * @api {put} /user/avatar Update User avatar image
     * @apiName PutUserAvatar
     * @apiGroup User
     *
     * @apiParam (Body parameter) {File} - Content of the image file
     * @apiParam (Header parameter) {String} Authorization The token for user in form of "Bearer {token}"
     * @apiParam (Header parameter) {String} Content-Type Mime type of the file (eg. image/jpeg)
     *
     * @apiSuccess {String} servingUrl Url of stored avatar image.
     *
     * @apiSuccessExample Success-Response:
     *     HTTP/1.1 200 OK
     *     {"servingUrl": "http://storage.googleapis.com/federatedbirds-storage/avatar-1.jpg"}
     *
     * @apiError invalidAuthorization Invalid authorization header format
     * @apiError invalidAuthorization Invalid token
     * @apiError mimetypeError There was an error in the Content-Type given as header
     * @apiError cannotSaveImage Wraps error coming from the data layer
     *
     * @apiErrorExample Error-Response:
     *     HTTP/1.1 401 Unauthorized
     *     {
     *          "status": 401,
     *          "code": "invalidAuthorization",
     *          "message": "Invalid authorization header format"
     *     }
     *
     */
    @Override
    protected Avatar doPut(HttpServletRequest req) throws ServletException, IOException, ApiException, GeneralSecurityException {
        if(getStringParameter(req, "avatar") != null) {

            try {
                // We store the authenticated user to avoid calling twice the method
                User authUser = getAuthenticatedUser(req);

                // We save the file to the storage using ImagesRepository
                // - the InputStream corresponds to the file content
                // - we must also specify the content type (eg. image/jpeg)
                // - then we have to specify a unique file id, here we user "avatar-{userId}"
                String savedFile = ImagesRepository.saveImage(
                        req.getInputStream(),
                        req.getHeader("Content-Type"),
                        "avatar-" + authUser.id
                );

                // Once the image has been uploaded we update the avatar URL for user
                authUser.avatar = savedFile;
                UsersRepository.saveUser(authUser);

                return new Avatar(savedFile);
            } catch(DataException e) {
                // We wrap the data level exception inside our ApiException
                // The purpose is to keep the data layer independant from the API layer
                throw new ApiException(
                        e.getError().status,
                        e.getError().code,
                        e.getError().message
                );
            } catch(MimeTypeException e) {
                // This exception happens if there is not possible match between Content-Type and file extension
                throw new ApiException(
                        415,
                        "mimetypeError",
                        e.getMessage()
                );
            }
        } else {
            return null;
        }
    }

    /**
     * @api {delete} /user Delete User
     * @apiName DeleteUser
     * @apiGroup User
     *
     * @apiParam (Header parameter) {String} Authorization The token for user in form of "Bearer {token}"
     *
     * @apiSuccess {Void} null null
     *
     * @apiSuccessExample Success-Response:
     *     HTTP/1.1 200 OK
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
     *
     */
    @Override
    protected Void doDelete(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // (OK) TODO: Security checks
        // (OK) TODO: Delete the user, the messages, the relationships

        // In fact the user retrieved through getAuthenticatedUser is necessarily
        // authorized to delete his own account so there is no need for security check here
        User authUser = getAuthenticatedUser(req);

        UsersRepository.deleteUser(authUser.id);

        // Then we delete the associated messages
        // This should be done normally at data layer but as of now we are not supposed to do it
        // so we do it by going through all the messages
        List<Message> allMessages = MessagesRepository.getMessages();
        for (Message message : allMessages) {
            if(message.user.get().id == authUser.id) {
                MessagesRepository.deleteMessage(message.id);
            }
        }

        // Then we delete the avatar image in repository
        try {
            if(ValidationUtils.validateImageUrl(authUser.avatar)) {
                String avatarFileName = authUser.avatar.substring(authUser.avatar.lastIndexOf('/') + 1);
                ImagesRepository.deleteImage(avatarFileName);
            }
        } catch (DataException e) {
            throw new ApiException(
                    e.getError().status,
                    e.getError().code,
                    e.getError().message
            );
        }

        return null;
    }

}