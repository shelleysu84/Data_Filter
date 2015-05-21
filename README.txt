Input:
./out/users.tsv
./out/recommendations.tsv
./out/pet_items.tsv
./out/high_roller_items.tsv
./out/male_items.tsv
./out/female_items.tsv
./out/not_available_in_california_items.tsv


Compile:
javac -cp ".:commons-cli-1.3.jar" Solution.java


Run:
java -cp ".:commons-cli-1.3.jar" Solution -c -g -p -r


usage: java Solution [-c] [-g] [-h] [-p] [-r]
Please provide at least one option

 -c,--notCA        Remove "not available in CA" items for CA users
 -g,--gender       Remove gender-inappropriate items
 -h,--help         Print this help message
 -p,--pet          Remove pet items for non pet owners
 -r,--highroller   Remove high roller items

Please report issues at ys1488@nyu.edu, Shelley Su


Output:
./new_recommendations.tsv
