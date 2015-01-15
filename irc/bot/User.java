package irc.bot;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map.Entry;

public class User {
	User(String name) { this(name, 0); }
	User(String name, int defaultlvl) {
		this.name = name;
		this.defaultlvl = defaultlvl;
		try { this.md = MessageDigest.getInstance("MD5"); } catch (Exception e) {} // since were using md5, this shouldn't throw exceptions
	}
	
	// TreeMaps dont work with int's, so i wrap it in a container
	private class IntContainer {
		IntContainer(int val) { setVal(val); }
		private int value;
		public void setVal(int val) { value = val; }
		public int getVal() { return value; }
	}
	private String name, /*qnick,*/ pass;
	private int defaultlvl;
	private HashMap<String, IntContainer> lvls = new HashMap<String, IntContainer>();
	private HashMap<String, User> groups = new HashMap<String, User>();
	private HashMap<String, String> variables = new HashMap<String, String>();
	private MessageDigest md;
	
	public String getName() { return name; }
	public int getLvl(String type) {
		IntContainer t = lvls.get(type);
		int lvl = defaultlvl;
		if ((t != null) && (t.getVal() > defaultlvl)) lvl = t.getVal();
		for (User u : groups.values()) {
			int i = u.getLvl(type);
			if (i > lvl) lvl = i;
		}
		return lvl;
	}
	public void addLvl(String type, int lvl) {
		lvls.put(type, new IntContainer(lvl));
	}
	public void deleteLvl(String type) {
		lvls.remove(type);
	}
	public void setLvl(String type, int lvl) {
		IntContainer t = lvls.get(type);
		if (t != null) t.setVal(lvl);
	}
	public int getDefaultLvl() { return defaultlvl; }
	public void setDefaultLvl(int lvl) { defaultlvl = lvl; }
	public HashMap<String, User> getGroups() { return groups; }
	public void addGroup(User group) { groups.put(group.getName(), group); }
	public void deleteGroup(User group) { groups.remove(group.getName()); }
	/*public String getQnick() {
		return qnick;
	}
	public void setQnick(String qnick) {
		this.qnick = qnick;
	}*/
	public String getVar(String handle) { return variables.get(handle); }
	public void setVar(String handle, String value) { variables.put(handle, value); }
	public void setPass(String hash) {
		this.pass = hash;
	}
	public void setMD5Pass(String pass) {
		this.pass = b2h(md(pass));
	}
	public boolean checkPass(String pass) {
		return this.pass.equals(b2h(md(pass)));
	}
	public byte[] md(byte[] in) { // use the initialized message digest
		return md.digest(in);
	}
	public byte[] md(String in) { return md(in.getBytes()); }
	public String b2h(byte[] in) { // byte array to hex string conversion
		int t;
		String str, out = "";
		for (int i = 0; i < in.length; i++) {
			if (in[i]>=0) t = in[i];
			else t = 256+in[i];
			str = Integer.toHexString(t);
			if (str.length() == 1) out += "0";
			out += str;
		}
		return out;
	}
	
	public String toXML(String indent, String it, boolean isuser) {
		String xml = "";
		String elemname = (isuser)?"user":"group";
		xml += indent+"<"+elemname+" name=\""+name+"\" defaultlvl=\""+defaultlvl+"\">\n";
		if (pass != null && !pass.isEmpty()) xml += indent+it+"<password>"+pass+"</password>\n";
		for (Entry<String, String> e : variables.entrySet()) {
			xml += indent+it+"<"+e.getKey()+">"+e.getValue()+"</"+e.getKey()+">\n";
		}
		//if (qnick != null && !qnick.isEmpty()) xml += indent+indenttype+"<qnick>"+qnick+"</qnick>\n";
		for (Entry<String, IntContainer> e : lvls.entrySet()) {
			xml += indent+it+"<entry name=\""+e.getKey()+"\" lvl=\""+e.getValue().getVal()+"\" />\n";
		}
		for (User u : groups.values()) {
			xml += indent+it+"<group name=\""+u.getName()+"\" />\n";
		}
		xml += indent+"</"+elemname+">\n";
		return xml;
	}
	/*
	  <user name="Proph" defaultlvl="100">
            <password>e2fc714c4727ee9395f324cd2e7f331f</password> <!-- abcd -->
            <qnick>Propheh</qnick>
            <entry name="usercommands" lvl="100" />
            <group name="admin" />
        </user>
	 */
	@Override
	public String toString() {
		String t = "User("+name+", "+defaultlvl+", levels[";
		int i = 1;
		for (Entry<String, IntContainer> e : lvls.entrySet()) {
			t += e.getKey()+":"+e.getValue().getVal();
			if (i < lvls.size()) t += ", ";
			i++;
		}
		t += "], groups[";
		i = 1;
		for (User u : groups.values()) {
			t += u.getName();
			if (i < groups.size()) t += ", ";
			i++;
		}
		t += "], variables[";
		i = 1;
		for (Entry<String, String> e : variables.entrySet()) {
			t += e.getKey()+": "+e.getValue();
			if (i < variables.size()) t += ", ";
			i++;
		}
		t += "])";
		return t;
	}
}
