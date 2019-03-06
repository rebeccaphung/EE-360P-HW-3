import java.net.*;
import java.io.*;
import java.util.*;

public class CarServer {

  public static int recordsCount = 0;

  public static  HashMap<Integer, ArrayList<String>> records = new HashMap<>(); //record #, customer name, car type, car color
  public static HashMap<String, ArrayList<Integer>> rentedCars = new HashMap<>(); //customer name, list of record numbers
  public static List<Car> availableCars = Collections.synchronizedList(new ArrayList<Car>());

  private ServerSocket serverSocket = null;

  public static void main (String[] args) {
    int tcpPort;
    int udpPort;
    if (args.length != 1) {
      System.out.println("ERROR: Provide 1 argument: input file containing initial inventory");
      System.exit(-1);
    }
    String fileName = args[0];
    tcpPort = 7000;
    udpPort = 8000;
    int len = 1024;

    // parse the inventory file

    Scanner sc = null;
    try {
      sc = new Scanner(new FileReader(fileName));

      while(sc.hasNextLine()) {
        String nextLine = sc.nextLine();
        String[] tokens = nextLine.split(" ");

        Car newCar = new Car(tokens[0], tokens[1], Integer.parseInt(tokens[2]));

        availableCars.add(newCar);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    // TODO: handle request from clients
    UDPServerThread udpServerThread = new UDPServerThread(udpPort, len);
    TCPServerThread tcpServerThread = new TCPServerThread(tcpPort);
    udpServerThread.start();
    tcpServerThread.start();



  }
 ////////////////////////////////////////////////////
  //car functions
  public static synchronized int rent(String customer, String brand, String color){

    for(Car c : availableCars){
      if(c.brand.equals(brand) && c.color.equals(color)){
        if(c.quantity != 0){
          recordsCount++;
          c.quantity--;

          if(rentedCars.containsKey(customer)){
            ArrayList<Integer> customerRecords = rentedCars.get(customer);
            customerRecords.add(recordsCount);
            rentedCars.put(customer, customerRecords);
          }
          else{
            ArrayList<Integer> customerRecords = new ArrayList<>();
            customerRecords.add(recordsCount);
            rentedCars.put(customer, customerRecords);
          }

          ArrayList<String> recordInfo = new ArrayList<>();
          recordInfo.add(customer);
          recordInfo.add(brand);
          recordInfo.add(color);
          records.put(recordsCount, recordInfo);

          return recordsCount;
        }
        else{
          return 0;
        }
      }
    }
    return -1;
  }

  public static synchronized boolean returnCar(int recordID){
    if(records.containsKey(recordID)){
      String customer = records.get(recordID).get(0);
      String brand = records.get(recordID).get(1);
      String color = records.get(recordID).get(2);
      for(Car c : availableCars){
        if(c.brand.equals(brand) && c.color.equals(color)) {
          c.quantity++;
        }
      }
      records.remove(recordID);

      ArrayList<Integer> customerRecords = rentedCars.get(customer);
      for(int i = 0; i < customerRecords.size(); i++){
        if(customerRecords.get(i) == recordID){
          customerRecords.remove(i);
          if(customerRecords.size() == 0){
            rentedCars.remove(customer);
          }
          return true;
        }
      }

    }
    return false;
  }

  public static synchronized String inventory(){
    String inventory = "";
    for(Car c : availableCars){
      inventory += c.brand + " " + c.color + " " + c.quantity + "\n";
    }
    inventory.substring(0, inventory.lastIndexOf("\n"));
    return inventory;
  }

  public static synchronized String list(String customer){
    if(!rentedCars.containsKey(customer)){
      return "No record found for " + customer;
    }
    else{
      String list = "";
      for(int recordID : rentedCars.get(customer)){
        ArrayList<String> recordInfo = records.get(recordID);
        list += recordID + " " + recordInfo.get(1) + " " + recordInfo.get(2) + "\n";
      }
      list.substring(0, list.lastIndexOf("\n"));
      return list;
    }
  }

  public static synchronized void exit(){

  }

  ///////////////////////////////////////////////////////////

  public static class UDPServerThread extends Thread{

    int port, len;

    public UDPServerThread(int port, int len){
      this.port = port;
      this.len = len;
    }
    @Override
    public void run() {
      DatagramPacket datapacket, returnpacket;

      try {
        DatagramSocket datasocket = new DatagramSocket(port);

        while (true) {

          byte[] buf = new byte[len];
          datapacket = new DatagramPacket(buf, buf.length);

          datasocket.receive(datapacket);

          String data = new String(datapacket.getData());

          InetAddress ip = datapacket.getAddress();

          String[] tokens = data.split(" ");

          if (tokens[0].equals("rent")) {
            int recordNum = rent(tokens[1], tokens[2], tokens[3]);
            if(recordNum == 0){
              String resp = "Request Failed - Car not available";
              sendUDPResponse(ip, port, resp, datasocket);
            }
            else if(recordNum == -1){
              String resp = "Request Failed - We do not have this car";
              sendUDPResponse(ip, port, resp, datasocket);
            }
            else{
              String resp = "Your request has been approved, " + recordNum + " " + tokens[1] + " " + tokens[2] + " " + tokens[3];
              sendUDPResponse(ip, port, resp, datasocket);
            }

          } else if (tokens[0].equals("return")) {
            int recordID = Integer.parseInt(tokens[1]);
            boolean returnCarSuccess = returnCar(recordID);
            if(returnCarSuccess){
              String resp = recordID + " is returned";
              sendUDPResponse(ip, port, resp, datasocket);
            }
            else{
              String resp = recordID + " not found, no such rental record";
              sendUDPResponse(ip, port, resp, datasocket);
            }

          } else if (tokens[0].equals("inventory")) {
            String resp = inventory();
            sendUDPResponse(ip, port, resp, datasocket);

          } else if (tokens[0].equals("list")) {
            String resp = list(tokens[1]);
            sendUDPResponse(ip, port, resp, datasocket);

          } else if (tokens[0].equals("exit")) {
            exit();
          }

        }
      } catch (Exception e) {
        System.err.println(e);
      }
    }
  }

  public static void sendUDPResponse(InetAddress ip, int port, String resp, DatagramSocket socket){
    byte[] data = resp.getBytes();
    DatagramPacket respPacket = new DatagramPacket(data, data.length, ip, port);
    try{
      socket.send(respPacket);
    }catch (Exception e){
      e.printStackTrace();
    }
  }

  /////////////////////////////////////////////////////////

  public static class TCPServerThread extends Thread{

    private Socket s;
    int port;

    public TCPServerThread(int port) {
      this.port = port;
    }
    public void run() {
      try {
        ServerSocket listener = new ServerSocket(port);
        Socket s;
        while ( (s = listener.accept()) != null) {
          new TCPThread(s);
        }
      } catch (IOException e) {
        System.err.println(e);
      }

    }
  }



  public static class TCPThread extends Thread{
    private Socket client;

    public TCPThread(Socket s){
      client = s;
      start();
    }

    @Override
    public void run() {
      BufferedReader req = null;
      PrintWriter resp = null;

      try{
        req = new BufferedReader(new InputStreamReader(client.getInputStream()));
        resp = new PrintWriter(client.getOutputStream(), true);
      }
      catch(Exception e){
        e.printStackTrace();
      }

      String nextLine;

      try{
        boolean exit = false;
        while(!exit){
          while((nextLine = req.readLine()) == null){}
          String[] tokens = nextLine.split(" ");

          if (tokens[0].equals("rent")) {
            int recordNum = rent(tokens[1], tokens[2], tokens[3]);
            if(recordNum == 0){
              resp.println("Request Failed - Car not available");
            }
            else if(recordNum == -1){
              resp.println("Request Failed - We do not have this car");
            }
            else{
              resp.println("Your request has been approved, " + recordNum + " " + tokens[1] + " " + tokens[2] + " " + tokens[3]);
            }

          } else if (tokens[0].equals("return")) {
            int recordID = Integer.parseInt(tokens[1]);
            boolean returnCarSuccess = returnCar(recordID);
            if(returnCarSuccess){
              resp.println(recordID + " is returned");
            }
            else{
              resp.println(recordID + " not found, no such rental record");
            }

          } else if (tokens[0].equals("inventory")) {
            String inventory = inventory();
            resp.println(inventory);

          } else if (tokens[0].equals("list")) {
            String list = list(tokens[1]);
            resp.println(list);

          } else if (tokens[0].equals("exit")) {
            exit();
          }

        }
      }
      catch(Exception e){
        e.printStackTrace();
      }



    }
  }
}
