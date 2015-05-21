import java.util.*;

public class Recommendation {
	long user_id;
	List<String> items;

	public Recommendation(long id, List<String> items) {
		this.user_id = id;
		this.items = items;
	}
}
