package Multicast;
/**
 * @author Lincoln Batista
 */
public class MainClass {
    public static void main(String args[]) {
        displayEntryMenu();
        MainProcess mp = new MainProcess();
        mp.run();
    }

    public static void displayEntryMenu () {
        System.out.println("- Type and press ENTER to send messages: ");
        System.out.println("- Type \"help\" to list the available commands\n");
    }
}