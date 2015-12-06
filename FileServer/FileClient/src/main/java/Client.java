import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.net.Socket;

/**
 * Created by Алексей on 03.12.2015.
 */
public class Client
{
    public static void main(String[] args)
    {
        try (Socket soc = new Socket("127.0.0.1", 5217))
        {
            TransferFileClient t = new TransferFileClient(soc);
            t.displayMenu();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

class TransferFileClient
{
    Socket ClientSoc;

    DataInputStream dis;
    DataOutputStream dos;
    BufferedReader br;

    TransferFileClient(Socket soc)
    {
        try
        {
            ClientSoc = soc;
            dis = new DataInputStream(ClientSoc.getInputStream());
            dos = new DataOutputStream(ClientSoc.getOutputStream());
            br = new BufferedReader(new InputStreamReader(System.in));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    void sendFile()
    {
        String filename;
        System.out.println("Enter File Name :");
        try
        {

            filename = br.readLine();

            File file = new File(filename);
            if (!file.exists())
            {
                System.out.println("File not Exists...");
                dos.writeUTF("File not found");
                return;
            }
            FileInputStream fis = new FileInputStream(file);
            String md5 = DigestUtils.md5Hex(fis);
            dos.writeUTF(md5);
            String msgFromServer = dis.readUTF();
            if (msgFromServer.compareTo("File Already Exists On Server") == 0)
            {
                System.out.println("File Already Exists On Server");
                return;
            } else if (msgFromServer.compareTo("ReseiveFile") == 0)
            {
                dos.writeUTF(file.getName());

                msgFromServer = dis.readUTF();
                if (msgFromServer.compareTo("File Already Exists") == 0)
                {
                    String Option;
                    System.out.println("File Already Exists. Want to OverWrite (Y/N) ?");
                    Option = br.readLine();
                    if (Option == "Y")
                    {
                        dos.writeUTF("Y");
                    } else
                    {
                        dos.writeUTF("N");
                        return;
                    }
                }

                System.out.println("Sending File...");

                int ch;
                do
                {
                    ch = fis.read();
                    dos.writeUTF(String.valueOf(ch));
                }
                while (ch != -1);
                fis.close();
                System.out.println(dis.readUTF());

            }
        } catch (IOException e)
        {
            e.printStackTrace();
            System.out.println("Invalid input");
            return;
        }
    }

    void findFile()
    {
        String filename;
        System.out.println("Enter File Name :");
        try
        {
            filename = br.readLine();

            dos.writeUTF(filename);
            String msgFromServer = dis.readUTF();
            if (msgFromServer.compareTo("File Not Found") == 0)
            {
                System.out.println("File not found on Server ...");
                return;
            } else
            {
                System.out.println(msgFromServer);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
            System.out.println("IOException");
        }
    }

    void receiveFile()
    {
        String fileKey;
        System.out.println("Enter File Key :");
        try
        {


            fileKey = br.readLine();
            dos.writeUTF(fileKey);
            String msgFromServer = dis.readUTF();
            if (msgFromServer.compareTo("File Not Found") == 0)
            {
                System.out.println("File not found on Server ...");
                return;
            } else
            {
                System.out.println("Receiving File ...");
                System.out.println("Enter Directory ...");
                String directory = br.readLine();
                File file = new File(directory + "\\" + msgFromServer);
                if (file.exists())
                {
                    String option;
                    System.out.println("File Already Exists. Want to OverWrite (Y/N)?");
                    option = br.readLine();
                    if (option == "N")
                    {
                        dos.flush();
                        return;
                    }
                }
                FileOutputStream fos = new FileOutputStream(file);
                int ch;
                String temp;
                do
                {
                    temp = dis.readUTF();
                    ch = Integer.parseInt(temp);
                    if (ch != -1)
                    {
                        fos.write(ch);
                    }
                } while (ch != -1);
                fos.close();
                System.out.println(dis.readUTF());

            }
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
            System.out.println("File Not Found");
        } catch (IOException e)
        {
            e.printStackTrace();
            System.out.println("IOException");
        }
    }

    void deleteFile()
    {
        String fileKey;
        System.out.println("Enter File Key :");
        try
        {


            fileKey = br.readLine();
            dos.writeUTF(fileKey);
            String msgFromServer = dis.readUTF();
            if (msgFromServer.compareTo("File Not Found") == 0)
            {
                System.out.println("File not found on Server ...");
                return;
            } else
            {
                System.out.println("Deleting File ...");
                String option;
                System.out.println("File Already Exists. Want to OverWrite (Y/N)?");
                option = br.readLine();
                if (option == "N")
                {
                    dos.flush();
                    return;
                } else
                {
                    dos.writeUTF("Y");
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
            System.out.println("IOException");
        }

    }

    public void displayMenu()
    {

        System.out.println("[ MENU ]");
        System.out.println("1. Send File");
        System.out.println("2. Find File");
        System.out.println("3. Receive File");
        System.out.println("4. Delete File");
        System.out.println("5. Exit");
        System.out.println("\nEnter Choice");
        while (true)
        {
            int choice;
            try
            {
                choice = Integer.parseInt(br.readLine());
                if (choice == 1)
                {
                    dos.writeUTF("SEND");
                    sendFile();
                } else if (choice == 2)
                {
                    dos.writeUTF("FIND");
                    findFile();
                } else if (choice == 3)
                {
                    dos.writeUTF("GET");
                    receiveFile();
                } else if (choice == 4)
                {
                    dos.writeUTF("DELETE");
                    deleteFile();
                } else if (choice == 5)
                {
                    dos.writeUTF("EXIT");
                    System.exit(1);
                } else
                {
                    System.out.println("Choice Not Found.\n" +
                            "Enter Choice ");
                    continue;
                }
            } catch (IOException e)
            {
                e.printStackTrace();
                System.out.println("IOException");
            }
        }
    }
}