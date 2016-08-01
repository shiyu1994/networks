/*
 * This file implements a stand alone client.
 */

package ClientServer;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.JTextField;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.awt.event.ActionEvent;
import java.io.InputStream;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

public class MyClient {

	private JFrame frame;
	private JTextField txtEnterHostName;
	private JTextField txtEnterFilename;
	private JButton btnClose;
	private JTextArea txtrAvailableFiles;

	private Socket clientSocket = null;
	private DataOutputStream outToServer = null;
	private BufferedInputStream fromServerContent = null;
	private BufferedReader fromServerInfo = null;
	private BufferedOutputStream downloadedFile = null;
	private String hostname = "localhost";
	private int port = 2001;
	private Boolean connected = false;
	private JTextField txtEnterPortNubmer;
	private JLabel lblAvailableFiles;
	private JLabel lblFileYouWant;
	private JTextField txtEnterDirectory;
	private JLabel Warn;
	
	/*
	 *Bind key listener for ESC key. 
	 */
	private KeyListener escapeListener = new KeyListener() {
		@Override
		public void keyTyped(KeyEvent e) {
			if(e.getKeyCode()==KeyEvent.VK_ESCAPE) {
				tearDown();
				System.exit(0);
			}
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if(e.getKeyCode()==KeyEvent.VK_ESCAPE) {
				tearDown();
				System.exit(0);
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			if(e.getKeyCode()==KeyEvent.VK_ESCAPE) {
				tearDown();
				System.exit(0);
			}
		}
		
	};
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MyClient window = new MyClient();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public MyClient() {
		initialize();
	}
	
	//Initialize UI
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 481, 360);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setFocusable(true);
		frame.addKeyListener(escapeListener);
		
		JButton btnConnect = new JButton("Connect");
		btnConnect.setFocusable(true);
		btnConnect.addKeyListener(escapeListener);
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(!connected) {
					hostname = txtEnterHostName.getText().trim();
					port = Integer.parseInt(txtEnterPortNubmer.getText());
					if(connect(hostname, port)) {
						connected = true;
						JLabel successConnected = new JLabel("Connect Success");
						successConnected.setBounds(137, 43, 197, 29);
						successConnected.addKeyListener(escapeListener);
						successConnected.setFocusable(true);
						frame.getContentPane().add(successConnected);
						btnConnect.setVisible(false);
					} 
					else {
						connected = false;
						JOptionPane.showMessageDialog(null, "Connection Failed\n Remeber to Start Server first?");
					}
				}
			}
		});
		btnConnect.setBounds(137, 43, 197, 29);
		frame.getContentPane().add(btnConnect);
		
		JButton btnNewButton = new JButton("Get File");
		btnNewButton.setFocusable(true);
		btnNewButton.addKeyListener(escapeListener);
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				requestFile(txtEnterFilename.getText().trim());
			}
		});
		btnNewButton.setBounds(179, 233, 117, 29);
		frame.getContentPane().add(btnNewButton);
		
		txtEnterHostName = new JTextField();
		txtEnterHostName.setText("Enter server IP");
		txtEnterHostName.setBounds(89, 6, 117, 28);
		txtEnterHostName.setFocusable(true);
		txtEnterHostName.addKeyListener(escapeListener);
		frame.getContentPane().add(txtEnterHostName);
		txtEnterHostName.setColumns(10);
		
		txtEnterFilename = new JTextField();
		txtEnterFilename.setText("Enter filename");
		txtEnterFilename.setBounds(207, 171, 257, 28);
		txtEnterFilename.setFocusable(true);
		txtEnterFilename.addKeyListener(escapeListener);
		frame.getContentPane().add(txtEnterFilename);
		txtEnterFilename.setColumns(10);
		
		btnClose = new JButton("Exit");
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tearDown();
				System.exit(0);
			}
		});
		btnClose.setFocusable(true);
		btnClose.addKeyListener(escapeListener);
		btnClose.setBounds(179, 289, 117, 29);
		frame.getContentPane().add(btnClose);
		
		txtEnterPortNubmer = new JTextField();
		txtEnterPortNubmer.setText("Enter port number");
		txtEnterPortNubmer.setBounds(259, 6, 128, 28);
		txtEnterPortNubmer.setFocusable(true);
		txtEnterPortNubmer.addKeyListener(escapeListener);
		frame.getContentPane().add(txtEnterPortNubmer);
		txtEnterPortNubmer.setColumns(10);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(17, 84, 447, 75);
		scrollPane.setFocusable(true);
		scrollPane.addKeyListener(escapeListener);
		frame.getContentPane().add(scrollPane);
		
		txtrAvailableFiles = new JTextArea();
		txtrAvailableFiles.setFocusable(true);
		txtrAvailableFiles.addKeyListener(escapeListener);
		scrollPane.setViewportView(txtrAvailableFiles);
		
		lblAvailableFiles = new JLabel("Available Files:");
		lblAvailableFiles.setFocusable(true);
		lblAvailableFiles.addKeyListener(escapeListener);
		lblAvailableFiles.setBounds(17, 60, 108, 16);
		frame.getContentPane().add(lblAvailableFiles);
		
		lblFileYouWant = new JLabel("File You Want:");
		lblFileYouWant.setFocusable(true);
		lblFileYouWant.addKeyListener(escapeListener);
		lblFileYouWant.setBounds(16, 177, 109, 16);
		frame.getContentPane().add(lblFileYouWant);
		
		JLabel lblDirectoryToPut = new JLabel("Directory To Put the File:");
		lblDirectoryToPut.setBounds(17, 205, 189, 16);
		lblDirectoryToPut.setFocusable(true);
		lblDirectoryToPut.addKeyListener(escapeListener);
		frame.getContentPane().add(lblDirectoryToPut);
		
		txtEnterDirectory = new JTextField();
		txtEnterDirectory.setFocusable(true);
		txtEnterDirectory.addKeyListener(escapeListener);
		txtEnterDirectory.setText(System.getProperty("user.dir") + File.separator + "download");
		File downloads = new File(System.getProperty("user.dir") + File.separator + "download");
		if(!downloads.exists()) 
			downloads.mkdir();
		txtEnterDirectory.setBounds(207, 199, 257, 28);
		frame.getContentPane().add(txtEnterDirectory);
		txtEnterDirectory.setColumns(10);
		
		Warn = new JLabel(" ");
		Warn.setFocusable(true);
		Warn.addKeyListener(escapeListener);
		Warn.setBounds(17, 275, 200, 16);
		frame.getContentPane().add(Warn);
	}
	
	//Tear down the connection from client side. Called when client exits. 
	public void tearDown() {
		try {
			clientSocket.shutdownInput();
			clientSocket.shutdownOutput();
			clientSocket.close();
		} catch(Exception e) {
			System.out.println("Exception: " + e.getMessage());
		}
	}
	
	//Connect to the server according to the provided IP address and port number.
	public Boolean connect(String hostname, int port) {
		try {
		clientSocket = new Socket(hostname, port);
		outToServer = new DataOutputStream(clientSocket.getOutputStream());
		InputStream fromServer = clientSocket.getInputStream();
		fromServerInfo = new BufferedReader(new InputStreamReader(fromServer));
		String fileList = "";
		String filename = "";
		//Server will first send the number of files in the current shared directory, as well as the 
		//list of the files.
		int fileNum = Integer.parseInt(fromServerInfo.readLine());
		outToServer.write(("get\n").getBytes());
		for(int i = 0; i < fileNum; ++i) {
			filename = fromServerInfo.readLine();
			fileList += filename + '\n';
			outToServer.write(("get\n").getBytes());
		}
		txtrAvailableFiles.setText(fileList);
		if(fileNum == 0) {
			txtrAvailableFiles.setText("No available file in current share directory of the server.\n");	
		}
		fromServerContent = new BufferedInputStream(fromServer);
		return true;
		} catch (Exception e) {
			System.out.println("Exception in connect(): " + e.getMessage());
			return false;
		}
	}
	
	//Request a file from the server. Store to the designated local directory.
	public void requestFile(String fileName) {
		try {
			Warn.setText("");
			outToServer.write((fileName + "\n").getBytes());
			outToServer.flush();
			String directory = txtEnterDirectory.getText().trim();
			if(!directory.endsWith(File.separator))
				directory += File.separator;
			File path = new File(directory);
			if(!path.exists()) 
				path.mkdir();
			byte[] buffer = new byte[1024];
			File getFile = new File(directory + fileName);
			int len = 0;
			int sum = 0;
			int expectedSize = -1;
			try {
				expectedSize = Integer.parseInt(fromServerInfo.readLine());
			}catch(Exception e) {
				getFile.delete();
				Warn.setText("No such file: " + fileName);
			}
			if(expectedSize >= 0) {
				outToServer.write("get size\n".getBytes());
				downloadedFile = new BufferedOutputStream(new FileOutputStream(getFile));
				while((len = fromServerContent.read(buffer)) != -1) {
					downloadedFile.write(buffer, 0, len);
					downloadedFile.flush();
					sum += len;
					if(sum == expectedSize)
						break;
				}
				downloadedFile.close();
			}
		} catch (Exception e) {
			System.out.println("Exception in requestFile(): " + e.getMessage());
		}
	}
}
