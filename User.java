public class User {
	long user_id;
	String state;
	String gender;
	String pet_owner;
	int estimated_clv;
	
	public User(long id, String state, String gender, String pet_owner, int clv) {
		this.user_id = id;
		this.state = state;
		this.gender = gender;
		this.pet_owner = pet_owner;
		this.estimated_clv = clv;
	}
}
