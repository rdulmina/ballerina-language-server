import ballerina/http;

type User record {
    string[] phoneNumber;
};

type Person record {
    string[] contacts;
};

import ballerina/http;

service / on new http:Listener(9090) {

    function init() returns error? {
    }

    resource function post getPerson(@http:Payload User user) returns Person|http:InternalServerError {
        do {
            User u1 = getUser();
            // Person var1 = {
            //                   contacts: [user.phoneNumber[0], user.phoneNumber[0]]
            //               };
            Person var3 = {
                                          contacts: ["123", "456"]
                                      };
        } on fail error e {
            return http:INTERNAL_SERVER_ERROR;
        }
    }
}