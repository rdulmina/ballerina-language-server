
# Configurable variable with integer value.
configurable int intVar = 42;
configurable string stringVar = "foo";

# Configurable variable with array value.
configurable string[] stringArrayVar = ["bar", "baz"];

# Configurable variable with record value.
configurable Student recordVar1 = {
    name: "John Doe",
    age: 20,
    grade: "A"
};
configurable Student recordVar2 = ?;

public type Student record {
    string name;
    int age;
    string grade;
};

public function main() {

}
