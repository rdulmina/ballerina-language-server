import ballerina/http;

type UserInfo record {|
   string username;
   string password;
|};

type Student record {|
   string username;
   string password;
|};

const string CONST = "CONST";

service OASServiceType on new http:Listener(9090) {

	resource function get pet() returns int|http:NotFound {
        do {
            UserInfo userInfo = {username: "un", password: "pw"};
            Credentials[] credentials = from var item in userInfo select { password: item.password };
		} on fail error e {
			return http:NOT_FOUND;
		}
	}
}
