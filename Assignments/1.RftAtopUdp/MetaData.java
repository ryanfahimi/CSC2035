import java.io.Serializable;

/* metadata to send to prepare for a file transfer */
public class MetaData implements Serializable {

	private static final long serialVersionUID = 1L;
	private String name; // name of the file to create on server
	private long size; // size of the file to send
	private int maxSegSize; // max payload size

	public int getMaxSegSize() {
		return maxSegSize;
	}

	public void setMaxSegSize(int maxSegSize) {
		this.maxSegSize = maxSegSize;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}
}
