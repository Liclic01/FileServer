import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Hashtable;
import java.util.Map;

/**
 * Класс создает соединение с клиентом.
 *
 */
public class Service
{
    static Hashtable<String, File> hashtable = new Hashtable<>();
    static NewDirectory nd;

    public static void main(String args[])
    {

        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
             ServerSocket soc = new ServerSocket(5217))
        {
            System.out.println("FTP Server on Port 5217");
            System.out.println("Enter Directory Name :");
            nd = new NewDirectory(br.readLine());
            getListFiles(nd.getDirectory());

            for (Map.Entry<String, File> pair : hashtable.entrySet())
            {
                System.out.println(pair.getKey() + " " + pair.getValue());
            }

            while (true)
            {
                System.out.println("Waiting for Connection...");
                TransferFile t = new TransferFile(soc.accept());
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }

    }
    /**
     * Метод заполняет hashtable ключами и соответствующими файлами
     *
     */

    public static void getListFiles(String str)
    {
        File f = new File(str);
        for (File s : f.listFiles())
        {
            if (s.isFile())
            {
                try (FileInputStream fis = new FileInputStream(s))
                {
                    String md5 = DigestUtils.md5Hex(fis);
                    hashtable.put(md5, s);
                } catch (FileNotFoundException e)
                {
                    e.printStackTrace();
                    System.out.println("File Not Found");
                } catch (IOException e)
                {
                    e.printStackTrace();
                    System.out.println("IOException");
                }
            } else if (s.isDirectory())
            {
                getListFiles(s.getAbsolutePath());
            }
        }

    }
}
/**
 * Класс осуществляет общение с клиентом через InputStream и OutputStream
 */
class TransferFile extends Thread
{
    Socket ClientSoc;
    DataInputStream dis;
    DataOutputStream dos;
    Hashtable<String, File> hashtable = Service.hashtable;


    TransferFile(Socket soc)
    {
        try
        {
            ClientSoc = soc;
            dis = new DataInputStream(ClientSoc.getInputStream());
            dos = new DataOutputStream(ClientSoc.getOutputStream());
            System.out.println("FTP Client Connected...");
            start();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    /**
     * Метод ищет файлы на сервере по имени и отправляет список(уникальный идентификатор/файл) клиенту
     */
    void findFile()
    {
        try
        {
            int count = 0;
            StringBuilder sb = new StringBuilder("");
            String filename = dis.readUTF();


            for (Map.Entry<String, File> pair : hashtable.entrySet())
            {
                if (pair.getValue().getName().contains(filename))
                {
                    sb.append(pair.getKey() + " " + pair.getValue().getName() + "\n");
                    count++;
                }
                if (count == 25)
                {
                    continue;
                }
            }
            if (count == 0)
            {
                dos.writeUTF("File Not Found");
                return;
            }
            dos.writeUTF(sb.toString().trim());

        } catch (IOException e)
        {
            e.printStackTrace();
            System.out.println("IOException");
        }

    }
    /**
     * Метод получает индификатор от клиента, и посылает его,
     * если данный файла существует
     */
    void sendFile()
    {
        try
        {
            String fileKey = dis.readUTF();


                if (!hashtable.containsKey(fileKey))
                {
                    dos.writeUTF("File Not Found");
                    return;
                } else
                {
                    File file = hashtable.get(fileKey);
                    dos.writeUTF(hashtable.get(fileKey).getName());
                    FileInputStream fis = new FileInputStream(file);
                    int ch;
                    do
                    {
                        ch = fis.read();
                        dos.writeUTF(String.valueOf(ch));
                    }
                    while (ch != -1);
                    fis.close();
                    dos.writeUTF("File Receive Successfully");
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
    /**
     * Метод получает индификатор от клиента и сохраняет в указанную директорию файл
     * с данным индификатором
     */
    void reseiveFile()
    {
        try
        {
            String md5 = dis.readUTF();
            if (hashtable.containsKey(md5))
            {
                dos.writeUTF("File Already Exists On Server");
                return;
            } else
            {
                dos.writeUTF("ReseiveFile");
            }
            String filename = dis.readUTF();

            if (filename.compareTo("File not found") == 0)
            {
                return;
            }
            File file = new File(Service.nd.getDirectory() + "\\" + filename);
            hashtable.put(md5, file);
            String option;
            if (file.exists())
            {
                dos.writeUTF("File Already Exists");
                option = dis.readUTF();
            } else
            {
                dos.writeUTF("SendFile");
                option = "Y";
            }
            if (option.compareToIgnoreCase("Y") == 0)
            {
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
                dos.writeUTF("File Send Successfully");

            } else
            {
                return;
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

    /**
     * Метод принимает индификатор от клиента и удаляет файл на сервере
     * с данным индификатором
     */
    void deleteFile()
    {
        try
        {
            String fileKey = dis.readUTF();
            String option;
            if (!hashtable.containsKey(fileKey))
            {
                dos.writeUTF("File Not Found");
                return;
            } else
            {
                dos.writeUTF("Delete File");
                option = dis.readUTF();
                if (option.compareToIgnoreCase("Y") == 0)
                {
                    File file = hashtable.get(fileKey);
                    file.delete();
                    hashtable.remove(fileKey);
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
            System.out.println("IOException");
        }

    }

    public void run()
    {
        while (true)
        {
            try
            {
                String command;
                System.out.println("Waiting for Command ...");
                try
                {
                    command = dis.readUTF();
                } catch (SocketException e)
                {
                    break;
                }
                if (command.compareTo("GET") == 0)
                {
                    System.out.println("\tGet Command Received ...");
                    sendFile();
                    continue;
                } else if (command.compareTo("SEND") == 0)
                {
                    System.out.println("\tSEND Command Received ...");
                    reseiveFile();
                    continue;
                } else if (command.compareTo("DELETE") == 0)
                {
                    System.out.println("\tDELETE Command Received ...");
                    deleteFile();
                    continue;
                } else if (command.compareTo("FIND") == 0)
                {
                    System.out.println("\tFIND Command Received ...");
                    findFile();
                    continue;
                } else if (command.compareTo("EXIT") == 0)
                {
                    System.out.println("\tDisconnect Command Received ...");
                    break;
                }
            } catch (IOException e)
            {
                e.printStackTrace();
                System.out.println("IOException");
            }
        }
    }
}
/**
 * Класс отвечает за выбор рабочей директории
 */
class NewDirectory
{
    private static NewDirectory instance;

    private String directory;


    public NewDirectory(String directory)
    {
        this.directory = directory;
    }

    public String getDirectory()
    {
        return directory;
    }

    private void setDirectory(String directory)
    {
        this.directory = directory;
    }
}