import java.io.Serializable;
import java.util.LinkedList;


public class Profile implements Serializable
{
	public static final boolean MALE = true;
	public static final boolean FEMALE = false;
	
	public final String name;
	public final int age;
	public final boolean gender;
	public final String occupation;
	public final LinkedList<String> interests;
	
	public Profile(String name, int age, boolean gender, String occupation, LinkedList<String> interests)
	{
		this.name = name;
		this.age = age;
		this.gender = gender;
		this.occupation = occupation;
		this.interests = interests;
	}
	
}
