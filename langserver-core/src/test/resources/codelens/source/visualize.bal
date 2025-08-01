import ballerina/module1;

type Person record {
    string name;
    int age;
};

int count = 0;

public function add(int a, int b) returns int {
    return a + b;
}

service / on new module1:Listener(9090) {
    private int[] items = [];

    isolated resource function resourceFn .() {
        var isOdd = self.items.'map(function(int n) returns boolean {
            return n % 2 != 0;
        });
    }

    isolated remote function remoteFn() {

    }

    function fn() {
        
    }
}

service class MyTypeObj1 {
    function init() {
    }

    resource function get name1() returns string {
        do {
            panic error("Unimplemented function");
        } on fail error err {
            //handle error
            panic error("Unhandled error");
        }
    }
}
