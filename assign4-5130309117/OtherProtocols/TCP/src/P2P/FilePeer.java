/*
 * This file implements a P2P file Peer, which can act simultaneously as a server and a client.
 */

package P2P;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.awt.event.ActionEvent;
import java.io.InputStream;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.Font;

public class FilePeer {

	private JFrame frame;
	private JTextField txtEnterHostName;
	private JTextField txtEnterFilename;
	private JButton btnClose;
	private JTextArea txtrAvailableFiles;
	
	private Boolean serverOn = false;
	private int portAsServer;
	private String shareDirectory;

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
	private JLabel lblNewLabel;
	private JLabel label;
	private JLabel label_1;
	private JLabel label_2;
	private JButton btnStartUpstream;
	private JTextField textField;
	private JLabel label_3;
	private JTextField textField_1;
	private JLabel lblNotePleaseMake;
	private JLabel lblBeforeStartServer;
	private JLabel label_6;
	private JLabel Warn;
	
	/*
	 *Bind key listener for ESC key. 
	 */
	private KeyListener escapeListener = new KeyListener() {
		@Override
		public void keyTyped(KeyEvent e) {
			if(e.getKeyCode()==KeyEvent.VK_ESCAPE) {
				System.exit(0);
			}
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if(e.getKeyCode()==KeyEvent.VK_ESCAPE) {
				System.exit(0);
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			if(e.getKeyCode()==KeyEvent.VK_ESCAPE) {
				System.exit(0);
			}
		}
		
	};
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					FilePeer window = new FilePeer();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public FilePeer() {
		initialize();
	}
	
	//Initialize UI
	private void initialize() {
		frame = new JFrame();
		frame.setFocusable(true);
		frame.addKeyListener(escapeListener);
		frame.setBounds(100, 100, 509, 571);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
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
						successConnected.setBounds(147, 246, 187, 29);
						successConnected.addKeyListener(escapeListener);
						successConnected.setFocusable(true);
						frame.getContentPane().add(successConnected);
						btnConnect.setVisible(false);
					} 
					else {
						JOptionPane.showMessageDialog(null, "Connection Failed");
					}
				}
			}
		});
		btnConnect.setBounds(147, 246, 187, 29);
		frame.getContentPane().add(btnConnect);
		
		JButton btnNewButton = new JButton("Get File");
		btnNewButton.setFocusable(true);
		btnNewButton.addKeyListener(escapeListener);
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				requestFile(txtEnterFilename.getText().trim());
			}
		});
		btnNewButton.setBounds(190, 436, 117, 29);
		frame.getContentPane().add(btnNewButton);
		
		txtEnterHostName = new JTextField();
		txtEnterHostName.setFocusable(true);
		txtEnterHostName.addKeyListener(escapeListener);
		txtEnterHostName.setText("Enter server IP");
		txtEnterHostName.setBounds(112, 218, 117, 28);
		frame.getContentPane().add(txtEnterHostName);
		txtEnterHostName.setColumns(10);
		
		txtEnterFilename = new JTextField();
		txtEnterFilename.setFocusable(true);
		txtEnterFilename.addKeyListener(escapeListener);
		txtEnterFilename.setText("Enter filename");
		txtEnterFilename.setBounds(227, 378, 257, 28);
		frame.getContentPane().add(txtEnterFilename);
		txtEnterFilename.setColumns(10);
		
		btnClose = new JButton("Exit");
		btnClose.setFocusable(true);
		btnClose.addKeyListener(escapeListener);
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tearDown();
				System.exit(0);
			}
		});
		btnClose.setBounds(190, 505, 117, 29);
		frame.getContentPane().add(btnClose);
		
		txtEnterPortNubmer = new JTextField();
		txtEnterPortNubmer.setFocusable(true);
		txtEnterPortNubmer.addKeyListener(escapeListener);
		txtEnterPortNubmer.setText("Enter port number");
		txtEnterPortNubmer.setBounds(260, 218, 128, 28);
		frame.getContentPane().add(txtEnterPortNubmer);
		txtEnterPortNubmer.setColumns(10);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setFocusable(true);
		scrollPane.addKeyListener(escapeListener);
		scrollPane.setBounds(17, 291, 461, 75);
		frame.getContentPane().add(scrollPane);
		
		txtrAvailableFiles = new JTextArea();
		txtrAvailableFiles.setFocusable(true);
		txtrAvailableFiles.addKeyListener(escapeListener);
		scrollPane.setViewportView(txtrAvailableFiles);
		
		lblAvailableFiles = new JLabel("Available Files:");
		lblAvailableFiles.setFocusable(true);
		lblAvailableFiles.addKeyListener(escapeListener);
		lblAvailableFiles.setBounds(17, 263, 108, 16);
		frame.getContentPane().add(lblAvailableFiles);
		
		lblFileYouWant = new JLabel("File You Want:");
		lblFileYouWant.setFocusable(true);
		lblFileYouWant.addKeyListener(escapeListener);
		lblFileYouWant.setBounds(17, 380, 109, 16);
		frame.getContentPane().add(lblFileYouWant);
		
		JLabel lblDirectoryToPut = new JLabel("Directory To Put the File:");
		lblDirectoryToPut.setFocusable(true);
		lblDirectoryToPut.addKeyListener(escapeListener);
		lblDirectoryToPut.setBounds(17, 408, 198, 16);
		frame.getContentPane().add(lblDirectoryToPut);
		
		txtEnterDirectory = new JTextField();
		txtEnterDirectory.setFocusable(true);
		txtEnterDirectory.addKeyListener(escapeListener);
		txtEnterDirectory.setText(System.getProperty("user.dir") + File.separator + "download");
		File downloads = new File(System.getProperty("user.dir") + File.separator + "download");
		if(!downloads.exists()) 
			downloads.mkdir();
		txtEnterDirectory.setBounds(227, 402, 257, 28);
		frame.getContentPane().add(txtEnterDirectory);
		txtEnterDirectory.setColumns(10);
		
		lblNewLabel = new JLabel("------------------------------------------------------------------------------------------------------------------------------------------------------------");
		lblNewLabel.setFocusable(true);
		lblNewLabel.addKeyListener(escapeListener);
		lblNewLabel.setBounds(-143, 199, 783, 16);
		frame.getContentPane().add(lblNewLabel);
		
		label = new JLabel("Please set the port before start. Change the share directory as you like.");
		label.setFocusable(true);
		label.addKeyListener(escapeListener);
		label.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		label.setBounds(6, 6, 458, 16);
		frame.getContentPane().add(label);
		
		try {
			label_1 = new JLabel("IP Address: " + InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		
		label_1.setFocusable(true);
		label_1.addKeyListener(escapeListener);
		label_1.setBounds(6, 34, 438, 16);
		frame.getContentPane().add(label_1);
		
		label_2 = new JLabel("Port Number: ");
		label_2.setFocusable(true);
		label_2.addKeyListener(escapeListener);
		label_2.setBounds(6, 62, 119, 16);
		frame.getContentPane().add(label_2);
		
		btnStartUpstream = new JButton("Start Server");
		btnStartUpstream.setFocusable(true);
		btnStartUpstream.addKeyListener(escapeListener);
		btnStartUpstream.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(!serverOn) {
					try {
						portAsServer = Integer.parseInt(textField.getText().trim());
						shareDirectory = textField_1.getText().trim();
						Thread server = new Thread(new TCPServer(portAsServer, shareDirectory));
						server.start();
						serverOn = true;
						label.setText("Server Running");
						btnClose.setText("Exit");
					} catch (Exception wrongPattern) {
						System.out.println(wrongPattern.getMessage());
						label.setText("Please enter a valid port number!");
					}
				}
				else {
					System.exit(0);
				}
			}
		});
		btnStartUpstream.setBounds(147, 172, 187, 29);
		frame.getContentPane().add(btnStartUpstream);
		
		textField = new JTextField();
		textField.setFocusable(true);
		textField.addKeyListener(escapeListener);
		textField.setText("Enter Port Number");
		textField.setColumns(10);
		textField.setBounds(132, 56, 134, 28);
		frame.getContentPane().add(textField);
		
		label_3 = new JLabel("Share Directory:");
		label_3.setFocusable(true);
		label_3.addKeyListener(escapeListener);
		label_3.setBounds(6, 90, 117, 16);
		frame.getContentPane().add(label_3);
		
		textField_1 = new JTextField();
		textField_1.setFocusable(true);
		textField_1.addKeyListener(escapeListener);
		shareDirectory = System.getProperty("user.dir") + File.separator + "share";
		File shares = new File(shareDirectory);
		if(!shares.exists()) 
			shares.mkdir();
		textField_1.setText(shareDirectory);
		textField_1.setColumns(10);
		textField_1.setBounds(132, 84, 312, 28);
		frame.getContentPane().add(textField_1);
		
		lblNotePleaseMake = new JLabel("Note: Please make sure the share directory contains some files ");
		lblNotePleaseMake.setFocusable(true);
		lblNotePleaseMake.addKeyListener(escapeListener);
		lblNotePleaseMake.setBounds(6, 112, 458, 16);
		frame.getContentPane().add(lblNotePleaseMake);
		
		lblBeforeStartServer = new JLabel("before start server. (subfolders are not seen as files) ");
		lblBeforeStartServer.setFocusable(true);
		lblBeforeStartServer.addKeyListener(escapeListener);
		lblBeforeStartServer.setBounds(45, 133, 419, 16);
		frame.getContentPane().add(lblBeforeStartServer);
		
		label_6 = new JLabel("");
		label_6.setFocusable(true);
		label_6.addKeyListener(escapeListener);
		label_6.setBounds(45, 155, 272, 16);
		frame.getContentPane().add(label_6);
		
		Warn = new JLabel(" ");
		Warn.setFocusable(true);
		Warn.addKeyListener(escapeListener);
		Warn.setBounds(17, 477, 200, 16);
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
		outToServer.write("get\n".getBytes());
		for(int i = 0; i < fileNum; ++i) {
			filename = fromServerInfo.readLine();
			fileList += filename + '\n';
			outToServer.write("get\n".getBytes());
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
			String directory = txtEnterDirectory.getText();
			File path = new File(directory);
			if(!path.exists()) 
				path.mkdir();
			if(!directory.endsWith(File.separator))
				directory += File.separator;
			File getFile = new File(directory + fileName);
			byte[] buffer = new byte[1024];
			int len = 0;
			int sum = 0;
			int expectedSize = -1;
			String s = fromServerInfo.readLine();
			try {
				expectedSize = Integer.parseInt(s);
			}catch(Exception e) {
				getFile.delete();
				System.out.println("Exception in requestFile(): " + e.getMessage());
				e.printStackTrace();
				Warn.setText("No such file: " + fileName);
			}
			if(expectedSize >= 0) {
				outToServer.write("get size\n".getBytes());
				downloadedFile = new BufferedOutputStream(new FileOutputStream(directory + fileName));
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
	
	//The Server thread. 
	static class TCPServer implements Runnable {

		private ServerSocket welcomeSocket = null;
		private Socket transfer = null;
		private BufferedReader fromClient = null;
		private String requestedFileName = "";
		private DataOutputStream toClientInfo = null;
		private BufferedOutputStream toClientContent = null;
		private int port = 0;
		private String shareDirectory;
		private Boolean finish = false;
		
		public TCPServer(int port, String shareDirectory) {
			this.port = port;
			this.shareDirectory = shareDirectory;
		}
		
		public void run() {
			initServer();
			//Accept connections from client, until being terminated.
			while(true) {
				waitingForConnection();
				do {
					getFileName();
					transfer();
				} while(!finish);
				try {
					transfer.close();
				} catch(Exception e) {
					System.out.println("Exception in run(): " + e.getMessage());
				}
			}
		}
		
		private void initServer() {
			try {
				welcomeSocket = new ServerSocket(port);
			} catch(Exception e) {
				System.out.println("Exception at initServer(): " + e.getMessage());
			}
		}
		
		private void waitingForConnection() {
			try {
				transfer = welcomeSocket.accept();
				fromClient = new BufferedReader(new InputStreamReader(transfer.getInputStream()));
				java.io.OutputStream toClient = transfer.getOutputStream();
				toClientInfo = new DataOutputStream(toClient);
				File directory = new File(shareDirectory);
				File[] list = directory.listFiles();
				int fileNum = 0;
				//Calculate number of available files in current shared directory.
				for(int i = 0; i < list.length; ++i) {
					if(list[i].isFile() && !list[i].getName().startsWith("."))
						++fileNum;
				}
				//toClientInfo.writeBytes(fileNum + "\n");
				toClientInfo.write((fileNum + "\n").getBytes());
				fromClient.readLine();
				//The server first transfers names of all the available files to the client.
				for(int i = 0; i < list.length; ++i) {
					if(list[i].isFile() && !list[i].getName().startsWith(".")) {
						//toClientInfo.writeBytes(list[i].getName() + "\n");
						toClientInfo.write((list[i].getName() + "\n").getBytes());
						fromClient.readLine();
					}
				}
				toClientContent = new BufferedOutputStream(toClient);
			} catch (Exception e) {
				System.out.println("Exception at waitingForConnection(): " + e.getMessage());
			}
		}
		
		//Get the requested file name from the client.
		private void getFileName() {
			try {
				requestedFileName = fromClient.readLine().trim();
			} catch (Exception e) {
				finish = true;
			}
		}
		
		//transfer file to the client.
		private void transfer() {
			try {
				if(!shareDirectory.endsWith(File.separator)) {
					shareDirectory += File.separator;
				}
				File file = new File(shareDirectory + requestedFileName);
				if(file != null && file.isFile() && file.exists()) {
					BufferedInputStream requestedFile = new BufferedInputStream(
							new FileInputStream(file));
					toClientInfo.write((((Long)file.length()).toString() + "\n").getBytes());
					fromClient.readLine();
					byte[] buffer = new byte[1024];
					int len = 0;
					while((len = requestedFile.read(buffer)) != -1) {
						toClientContent.write(buffer, 0, len);
						toClientContent.flush();
					}
					requestedFile.close();
				}
				else {
					toClientInfo.write(("noSuchFile\n").getBytes());
				}
			} catch (Exception e) {
				finish = true;
			} 
		}
	}
}
