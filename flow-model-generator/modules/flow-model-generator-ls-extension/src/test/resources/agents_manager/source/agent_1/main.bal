import ballerina/http;
import ballerinax/ai;

ai:OpenAiProvider chatGptModelAgent = check new ("", ai:GPT_4O);

public function main() {
    int x = 32;
    while x < 50 {
        if (x % 2 == 0) {
            int y = 12;
        } else {
            string z = "hello";
            do {
                if z.length() == x {
                    Address address = {houseNo: "10", line1: "foo", line2: "bar", city: "Colombo", country: "Sri Lanka"};

                } else {
                    fail error("error");
                }
            } on fail {
                break;
            }
        }
        x += 2;
    }
}

@ai:AgentTool
isolated function fn(int x) returns int {
    return x + 1;
}

@ai:AgentTool
isolated function fn2(int x) returns int? {
    return x + 1;
}

@ai:AgentTool
function fn1(int x) returns int|error? {
    return x + 1;
}

http:Client httpClientResult = check new ("http://localhost:9091");

final Address[] addresses = [];
final Address var1 = {country: "", city: "", houseNo: "", line2: "", line1: ""};
