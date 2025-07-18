import ballerina/http;

type UserInfo record {|
   string username;
   string password;
|};

type Student record {|
   string username;
   string password;
   int length;
|};

const string CONST = "CONST";

service OASServiceType on new http:Listener(9090) {

	resource function get pet() returns int|http:NotFound {
        do {
            UserInfo userInfo = {username: "un", password: "pw"};

		} on fail error e {
			return http:NOT_FOUND;
		}
	}
}
