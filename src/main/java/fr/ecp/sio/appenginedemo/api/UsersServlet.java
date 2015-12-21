package fr.ecp.sio.appenginedemo.api;

import fr.ecp.sio.appenginedemo.data.UsersRepository;
import fr.ecp.sio.appenginedemo.model.User;
import fr.ecp.sio.appenginedemo.utils.MD5Utils;
import fr.ecp.sio.appenginedemo.utils.TokenUtils;
import fr.ecp.sio.appenginedemo.utils.ValidationUtils;
import org.apache.commons.codec.digest.DigestUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

/**
 * A servlet to handle all the requests on a list of users
 * All requests on the exact path "/users" are handled here.
 */
public class UsersServlet extends JsonServlet {

    /**
     * @api {get} /users/:id/[followed|followers] List Users
     * @apiName GetUsers
     * @apiGroup User
     *
     * @apiParam (Header parameter) {String} Authorization The token for user in form of "Bearer {token}"
     * @apiParam (Header parameter) {String} limit Limit of users retrieved per request (for pagination)
     * @apiParam (Header parameter) {String} continuationTolen Token to get the next page (for pagination)
     * @apiParam (Url parameter) {Number} id Users unique ID. This ID can be "me" for currently authenticated user.
     * @apiParam (Url parameter) {String} followed|followers Type of users relations to retrieve (can be "followed" or "followers"). All users if not specified.
     *
     * @apiSuccess {Array} array List of all users by default. If "followed" or "followers" parameters are specified, then list of corresponding users only.
     * @apiSuccess {String} login Login of the User.
     * @apiSuccess {String} avatar Avatar of the User.
     * @apiSuccess {String} coverPicture Cover picture of the User.
     * @apiSuccess {String} email Email of the User (hidden if is different from authenticated user).
     * @apiSuccess {String} password Password hash of the user.
     *
     * @apiSuccessExample Success-Response:
     *     HTTP/1.1 200 OK
     *     [
     *           {
     *             "id": 1,
     *             "login": "user1",
     *             "avatar": "http://storage.googleapis.com/federatedbirds-storage/avatar-1.jpg",
     *             "coverPicture": "",
     *             "email": "user1@yopmail.com",
     *             "password": "0b14d501a594442a01c6859541bcb3e8164d183d32937b851835442f69d5c94e"
     *           },
     *           {
     *             "id": 1000001,
     *             "login": "user3",
     *             "avatar": "http://www.gravatar.com/avatar/09e7572bb8bb7f8327302cd6fdb30e28?d=wavatar",
     *             "coverPicture": "",
     *             "email": "user3@yopmail.com",
     *             "password": "bc4f7f75a897a48c660a15077318226a17d01b320dea8aacf63f1b29fcfcf2a1"
     *           },
     *           {
     *             "id": 2000001,
     *             "login": "user4",
     *             "avatar": "http://www.gravatar.com/avatar/b590919a75487a843e13bcc99e8e9fc6?d=wavatar",
     *             "coverPicture": "",
     *             "email": "user4@yopmail.com",
     *             "password": "a75c2000f6ca4b31cba2c85fd2c7ab7582967ffb929ce938e137b0f4dd15a930"
     *           }
     *         ]
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
    @Override
    protected List<User> doGet(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // TODO: define parameters to search/filter users by login, with limit, order...
        // (OK) TODO: define parameters to get the followings and the followers of a user given its id
        // (OK) TODO: define parameters to get the followings and the followers of a user given its id

        // We filter the action to perform depending on the parameters as defined by rewrite rules :
        // - followedBy
        // - followerOf
        //
        // If no such parameter is found in URL, we return all users.

        // URL params from rewrite rule : id
        // URL params from request : limit, continuationToken
        Integer limit = getIntegerParameter(req, "limit");
        String continuationToken = getStringParameter(req, "continuationToken");

        // GET /users/{id}/followed => /users?id={id}&followedBy
        if(hasParameter(req, "followedBy")) {
            return UsersRepository.getUserFollowed(getIdParameter(req), limit, continuationToken).users;
        }

        // GET /users/{id}/follower => /users?id={id}&followerOf
        if(hasParameter(req, "followerOf")) {
            return UsersRepository.getUserFollowers(getIdParameter(req), limit, continuationToken).users;
        }

        // By default, return all users
        return UsersRepository.getUsers(null, null).users;
    }

    /**
     * @api {post} /users Create User
     * @apiName PostUsers
     * @apiGroup User
     *
     * @apiParam (Header parameter) {String} Authorization The token for user in form of "Bearer {token}"
     *
     * @apiSuccess {String} token Newly generated token for user.
     *
     * @apiSuccessExample Success-Response:
     *     HTTP/1.1 200 OK
     *     "eyJhbGciOiJIUzUxMiJ9.eyJqdGkiOiIxIn0.Wekkpa98Ll3qzPsxy7iX-yDN2p-QlLPT5u_73vBwjMHaPwHGdS7-LRKjtx0Rvjsa8YRgKRAtqAWcy2FhTMVw7g"
     *
     * @apiError invalidRequest Invalid JSON body
     * @apiError invalidLogin Login did not match the specs
     * @apiError invalidPassword Password did not match the specs
     * @apiError duplicateLogin Duplicate login
     * @apiError duplicateEmail Login Duplicate email
     *
     * @apiErrorExample Error-Response:
     *     HTTP/1.1 400 Bad request
     *     {
     *          "status": 400,
     *          "code": "invalidRequest",
     *          "message": "Invalid JSON body"
     *     }
     */
    @Override
    protected String doPost(HttpServletRequest req) throws ServletException, IOException, ApiException {

        // The request should be a JSON object describing a new user
        User user = getJsonRequestBody(req, User.class);
        if (user == null) {
            throw new ApiException(400, "invalidRequest", "Invalid JSON body");
        }

        // Perform all the usul checkings
        if (!ValidationUtils.validateLogin(user.login)) {
            throw new ApiException(400, "invalidLogin", "Login did not match the specs");
        }
        if (!ValidationUtils.validatePassword(user.password)) {
            throw new ApiException(400, "invalidPassword", "Password did not match the specs");
        }
        if (!ValidationUtils.validateEmail(user.email)) {
            throw new ApiException(400, "invalidEmail", "Invalid email");
        }
        if (UsersRepository.getUserByLogin(user.login) != null) {
            throw new ApiException(400, "duplicateLogin", "Duplicate login");
        }
        if (UsersRepository.getUserByEmail(user.email) != null) {
            throw new ApiException(400, "duplicateEmail", "Duplicate email");
        }

        // Explicitly give a fresh id to the user (we need it for next step)
        user.id = UsersRepository.allocateNewId();

        // (OK) TODO: find a solution to receive an store profile pictures
        // Simulate an avatar image using Gravatar API
        user.avatar = "http://www.gravatar.com/avatar/" + MD5Utils.md5Hex(user.email) + "?d=wavatar";

        // Hash the user password with the id a a salt
        user.password = generateUserPassword(user);

        // Persist the user into the repository
        UsersRepository.saveUser(user);

        // Create and return a token for the new user
        return TokenUtils.generateToken(user.id);
    }
}