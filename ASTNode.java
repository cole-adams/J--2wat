import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashMap;

public class ASTNode {
	public String construct;
	public ArrayList<ASTNode> children;
	public ASTNode parent;
	public HashMap<String, Object> attributes;
	public int level;
	public int lineno;

	public ASTNode(String construct, List<ASTNode> children) {
		this.construct = construct;
		this.children = new ArrayList<ASTNode>(children);
		this.attributes = new HashMap<String, Object>();
	}

	public ASTNode(String construct) {
		this.construct = construct;
		this.children = new ArrayList<ASTNode>();
		this.attributes = new HashMap<String, Object>();
	}

	public ASTNode(String construct, ASTNode child) {
		this.construct = construct;
		this.children = new ArrayList<ASTNode>();
		this.children.add(child);
		this.attributes = new HashMap<String, Object>();
	}

	public ASTNode(String construct, ASTNode child, int lineno) {
		this.construct = construct;
		this.children = new ArrayList<ASTNode>();
		this.children.add(child);
		this.attributes = new HashMap<String, Object>();
		attributes.put("lineno", lineno);
		this.lineno = lineno;
	}

	public ASTNode(String construct, List<ASTNode> children, int lineno) {
		this.construct = construct;
		this.children = new ArrayList<ASTNode>(children);
		this.attributes = new HashMap<String, Object>();
		attributes.put("lineno", lineno);
		this.lineno = lineno;
	}

	public ASTNode(String construct, int lineno) {
		this.construct = construct;
		this.children = new ArrayList<ASTNode>();
		this.attributes = new HashMap<String, Object>();
		attributes.put("lineno", lineno);
		this.lineno = lineno;
	}

	public ASTNode(String construct, String val, int lineno) {
		this.construct = construct;
		this.children = new ArrayList<ASTNode>();
		this.attributes = new HashMap<String, Object>();
		attributes.put("lineno", lineno);
		this.lineno = lineno;
		attributes.put("attr", val);
	}

	public String toString() {
		Set<String> keySet = attributes.keySet();
		StringBuilder sb = new StringBuilder();
		sb.append(construct);

		if (!keySet.isEmpty()) {
			sb.append(" {");
			for (String s : keySet) {
				sb.append("'");
				sb.append(s);
				sb.append("': ");
				sb.append(attributes.get(s));
				sb.append(",");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append("}");
		}
		return sb.toString();
	}

	public String treeToString() {
		StringBuilder st = new StringBuilder();
		st = nodeToString(st, 0);
		return st.toString();
	}

	public StringBuilder nodeToString(StringBuilder st, int depth) {
		for (int i = 0; i < depth; i++) {
			st.append("\t");
		}
		st.append(this.toString());
		st.append("\n");
		for (ASTNode child : children) {
			st = child.nodeToString(st, depth+1);
		}
		return st;
	}

	public ASTNode addChild(ASTNode n) {
		children.add(n);
		return this;
	}

	public void setParent(ASTNode p) {
		this.parent = p;
	}

	public void attachParents(int l) {
		this.level = l;
		for (int i = 0; i < children.size(); i++) {
			ASTNode c = children.get(i);
			c.setParent(this);
			c.attachParents(l+1);
		}
	}

	public void addAttribute(String name, Object o) {
		attributes.put(name, o);
	}

	public Object getAttribute(String key) {
		return attributes.get(key);
	}

	public LinkedList<ASTNode> postOrderTraversal() {
		if (children.size() == 0) {
			LinkedList<ASTNode> po = new LinkedList<ASTNode>();
			po.add(this);
			return po;
		}

		LinkedList<ASTNode> po = children.get(0).postOrderTraversal();
		for (int i = 1; i < children.size(); i++) {
			po.addAll(children.get(i).postOrderTraversal());
		}
		po.add(this);

		return po;
	}

	public LinkedList<ASTNode> prePostOrderTraversal() {
		LinkedList<ASTNode> pre = new LinkedList<ASTNode>();
		pre.add(this);
		if (children.size() == 0) {
			return pre;
		}
		for (int i = 0; i < children.size(); i++) {
			pre.addAll(children.get(i).prePostOrderTraversal());
		}

		pre.add(this);
		return pre;
	}

	public LinkedList<ASTNode> preOrderTraversal() {
		LinkedList<ASTNode> pre = new LinkedList<ASTNode>();
		pre.add(this);
		if (children.size() == 0) {
			return pre;
		}
		for (int i = 0; i < children.size(); i++) {
			pre.addAll(children.get(i).preOrderTraversal());
		}
		return pre;
	}
}
