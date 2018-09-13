package Multicast;

public class MainClass {
    public static void main(String args[]) {
        displayEntryMenu();
        MainProcess mp = new MainProcess();
        mp.run();
    }

    public static void displayEntryMenu () {
        System.out.println("- Just type and press ENTER to send messages: ");
        System.out.println("- Type \"help\" to see the list of available commands\n");
    }
}