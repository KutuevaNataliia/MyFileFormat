import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        MyFile.makeFile("test1.me", "A+very+simple+text+unicode", "UNICODE");
        System.out.println(MyFile.readFile("test1.me"));
    }
}
