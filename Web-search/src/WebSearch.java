import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.StringTokenizer;

// You should call this code as follows:
//
//   java WebSearch directoryName searchStrategyName
//   (or jview, in J++)
//
//   where <directoryName> is the name of corresponding intranet
//   and <searchStrategyName> is one of {breadth, depth, best, beam}.

public class WebSearch {
	static LinkedList<SearchNode> OPEN;
	static HashSet<String> CLOSED = new HashSet<String>();
	static final boolean DEBUGGING = true; // When set, WARNING: lots of info is
											// printed.

	// If searchStrategy = "beam", limit the size of OPEN to this value.
	// The setSize() method in the Vector class can be used to accomplish this.
	static int beamWidth = 2;

	static final String START_NODE = "page1.html";

	// A web page is a goal node if it includes the following string.
	static final String GOAL_PATTERN = "QUERY1 QUERY2 QUERY3 QUERY4";

	public static enum Type {
		BREADTH, DEPTH, BEST, BEAM
	}

	static WebSearch.Type type = null;

	public static void main(String args[]) {
		if (args.length != 2) {
			System.out.println("You must provide the directoryName and searchStrategyName.  Please try again.");
		} else {
			String directoryName = args[0];
			String searchStrategyName = args[1];

			if (searchStrategyName.equalsIgnoreCase("breadth")) {
				type = WebSearch.Type.BREADTH;
			} else if (searchStrategyName.equalsIgnoreCase("depth")) {
				type = WebSearch.Type.DEPTH;
			} else if (searchStrategyName.equalsIgnoreCase("best")) {
				type = WebSearch.Type.BEST;
			} else if (searchStrategyName.equalsIgnoreCase("beam")) {
				type = WebSearch.Type.BEAM;
			} else {
				System.out.println("The valid search strategies are:");
				System.out.println("  BREADTH DEPTH BEST BEAM");
			}
			performSearch(directoryName);
		}
		Utilities.waitHere("Press ENTER to exit.");
	}

	static void performSearch(String directoryName) {
		if (type == null) return;
		int nodesVisited = 0;
		OPEN = new LinkedList<SearchNode>();
		OPEN.add(new SearchNode(START_NODE, "", null, null));

		while (!OPEN.isEmpty()) {
			int i = 0;
			do {
				if (visitNode(pop(OPEN), directoryName)) 
					break;
				nodesVisited++;
				i++;
			} while (type == WebSearch.Type.BEAM && i < beamWidth);
			if (DEBUGGING)
				System.out.println("Nodes visited = " + nodesVisited + " |OPEN| = " + OPEN.size());
		}
		System.out.println(" Visited " + nodesVisited + " nodes, starting @" + " " + directoryName + File.separator
				+ START_NODE + ", using: " + type + " search.");
	}

	static boolean visitNode(SearchNode currentNode, String directoryName) {
		String currentURL = currentNode.getNodeName();
		String contents = Utilities.getFileContents(directoryName + File.separator + currentURL);
		if (isaGoalNode(contents)) {
			currentNode.reportSolutionPath();
			return true;
		}
		currentNode.setContents(contents);
		CLOSED.add(currentURL);
		addNewChildrenToOPEN(currentNode);
		return false;
	}

	static void addNewChildrenToOPEN(SearchNode parent) {
		// Check if type is beam, don't add children right away
		StringTokenizer st = new StringTokenizer(parent.getContents());
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.equalsIgnoreCase("<A")) {
				String hyperlink;
				if (DEBUGGING)
					System.out.println("Encountered a HYPERLINK");
				token = st.nextToken();
				if (!token.equalsIgnoreCase("HREF")) {
					System.out.println("Expecting 'HREF' and got: " + token);
				}
				token = st.nextToken();
				if (!token.equalsIgnoreCase("=")) {
					System.out.println("Expecting '=' and got: " + token);
				}
				// Name of file being linked to.
				hyperlink = st.nextToken();
				if (!hyperlink.startsWith("page")) {
					System.out.println("Expecting 'page#.html' and got: " + hyperlink);
				}
				token = st.nextToken();
				if (!token.equalsIgnoreCase(">")) {
					System.out.println("Expecting '>' and got: " + token);
				}
				if (DEBUGGING)
					System.out.println(" - found a link to " + hyperlink);
				// Have collected a child node; now have to decide what to do with it
				if (alreadyInOpen(hyperlink)) {
					if (DEBUGGING)
						System.out.println(" - this node is in the OPEN list.");
				} else if (CLOSED.contains(hyperlink)) {
					if (DEBUGGING)
						System.out.println(" - this node is in the CLOSED list.");
				} else {
					String hypertext = "";
					do {
						token = st.nextToken();
						if (!token.equalsIgnoreCase("</A>"))
							hypertext += " " + token;
					} while (!token.equalsIgnoreCase("</A>"));

					if (DEBUGGING)
						System.out.println("   with hypertext: " + hypertext);
					insert(OPEN, new SearchNode(hyperlink, hypertext, parent, null));
				}
			}
		}
	}

	static boolean isaGoalNode(String contents) {
		return (contents != null && contents.indexOf(GOAL_PATTERN) >= 0);
	}

	// Adapt for use when inserting nodes into OPEN
	// according to their heuristic score.
	static boolean alreadyInOpen(String hyperlink) {
		int length = OPEN.size();
		for (int i = 0; i < length; i++) {
			SearchNode node = OPEN.get(i);
			String oldHyperlink = node.getNodeName();

			if (hyperlink.equalsIgnoreCase(oldHyperlink))
				return true;
		}

		return false;
	}

	static boolean insert(LinkedList<SearchNode> list, SearchNode node) {
		switch (type) {
		case BREADTH:
		case BEST:
			insertPriorityQueue(list, node);
			break;
		case BEAM:
			list.addLast(node);
			break;
		case DEPTH:
			list.addFirst(node);
			break;
		}
		return true;
	}

	static boolean insertPriorityQueue(LinkedList<SearchNode> list, SearchNode node) {
		//////////////////////////////////////////////////////////////////////
		// arguments both "hypertext" (ie, the text associated with this
		// hyperlink) and "contents" (ie, the full text of the current page).
		//////////////////////////////////////////////////////////////////////
		// HINT: read about the insertElementAt() and addElement()
		// methods in the Vector class.
		double hvalue = node.computeHvalue(GOAL_PATTERN);
		for (SearchNode current : list) {
			if (current.computeHvalue(GOAL_PATTERN) > hvalue) {
				list.add(list.indexOf(current), node);
				return true;
			}
		}
		list.addLast(node);
		return true;
	}

	static SearchNode pop(LinkedList<SearchNode> list) {
		return list.removeFirst();
	}
}

class SearchNode {
	private SearchNode parent;
	final String nodeName;
	private double hvalue;
	private String contents; 
	private String hypertext;
	

	public SearchNode(String link, String hypertext, SearchNode parent, String contents) {
		nodeName = link;
		this.hypertext = hypertext;
		this.parent = parent;
		this.contents = null;
	}
	

	public void reportSolutionPath() {
		int count = 1;
		System.out.print("Solution path: " + this.nodeName);
		SearchNode parent = this.parent;
		while (parent != null) {
			System.out.print("-" + parent.getNodeName());
			parent = this.parent;
			count++;
		}
		System.out.println("Path size: " + count);
	}

	public String getNodeName() {
		return nodeName;
	}

	public SearchNode getParent() {
		return parent;
	}

	public void setParent(SearchNode parent) {
		this.parent = parent;
	}
	

	public double getHvalue() {
		return hvalue;
	}

	public String getContents() {
		return this.contents;
	}
	
	public void setContents(String contents) {
		this.contents = contents;
	}

	public double computeHvalue(String goalPattern) {
		String[] goals = goalPattern.split(" ");
		for (int i = 0; i < goals.length; i++) {
			String goal = goals[i];
			if (contents != null) {
				int index = contents.indexOf(goal);
				if (index >= 0) {
					this.hvalue++;
					
					int j = i+1;
					String goalSequence = goal;
					while (j < goals.length) {
						goalSequence += " " + goals[j];
						if (contents.indexOf(goalSequence) >= 0) {
							this.hvalue++;
						}
						j++;
					}
				}
				
				index = hypertext.indexOf(goal);
				if (index >= 0)
					this.hvalue++;
			}
		}
		return this.hvalue;
	}
}

// Some 'helper' functions follow. You needn't understand their internal
// details.
class Utilities {
	// In J++, the console window can close up before you read it,
	// so this method can be used to wait until you're ready to proceed.
	public static void waitHere(String msg) {
		System.out.println("");
		System.out.println(msg);
		try {
			System.in.read();
		} catch (Exception e) {
		} // Ignore any errors while reading.
	}

	// This method will read the contents of a file, returning it
	// as a string. (Don't worry if you don't understand how it works.)
	public static synchronized String getFileContents(String fileName) {
		File file = new File(fileName);
		String results = null;
		try {
			int length = (int) file.length(), bytesRead;
			byte byteArray[] = new byte[length];

			ByteArrayOutputStream bytesBuffer = new ByteArrayOutputStream(length);
			FileInputStream inputStream = new FileInputStream(file);
			bytesRead = inputStream.read(byteArray);
			bytesBuffer.write(byteArray, 0, bytesRead);
			inputStream.close();

			results = bytesBuffer.toString();
		} catch (IOException e) {
			System.out.println("Exception in getFileContents(" + fileName + "), msg=" + e);
		}
		return results;
	}
}
