import ballerina/http;

type UserInfo record {|
   string username;
   string password;
|};

type Student record {|
   string username;
   string password;
|};

type Department record {|
    string name;
    Student[] students;
|};

const string CONST = "CONST";

service OASServiceType on new http:Listener(9090) {

	resource function get pet() returns int|http:NotFound {
        do {
            Department department = {name: "DEPT1", students: [{}]};
            Department department1 = {name: "DEPT1", students: [{username: "UNAME"}]};
		} on fail error e {
			return http:NOT_FOUND;
		}
	}
}
