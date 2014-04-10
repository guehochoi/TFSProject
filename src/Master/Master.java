package Master;

public class Master {

	public static void main(String[] args) {
		FileSystem fs = new FileSystem();
		fs.createFile("\\kitten.txt");
		fs.createDirectory("\\usr");
		fs.createDirectory("\\usr\\bin");
		fs.createDirectory("\\usr\\bin\\test");
		fs.createFile("\\usr\\bin\\test\\vim4lyfe.txt");
		fs.createDirectory("\\share");
		fs.createDirectory("\\lib");
		fs.createFile("\\usr\\kitten.txt");
		fs.createFile("ShouldFail.txt");
		fs.backupFS();
		fs.printDirectory("\\", 0);
	}
}
