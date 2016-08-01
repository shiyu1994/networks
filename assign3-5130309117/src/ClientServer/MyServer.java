/*
 * This file implements a stand alone file server.
 */


package ClientServer;
import java.awt.EventQueue;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JFrame;
import javax.swing.JLabel;

import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.ActionEvent;
import java.awt.Font;

public class MyServer {

	private JFrame frame;

	static MyServer MyServer = new MyServer();
	private JTextField txtEnterPortNumber;
	
	private Boolean serverOn = false;
	private int port;
	private String shareDirectory;
	private static JTextField txtEnterShareDirectory;
	
	//Create key listener for ESC.
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
					MyServer window = new MyServer();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}


	public MyServer() {
		initialize();
	}

	//Initialize the UI
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 477, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setFocusable(true);
		frame.addKeyListener(escapeListener);
		
		JLabel lblServerRunning = new JLabel("Please set the port before start. Change the share directory as you like.");
		lblServerRunning.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		lblServerRunning.setBounds(6, 18, 438, 16);
		lblServerRunning.setFocusable(true);
		lblServerRunning.addKeyListener(escapeListener);
		frame.getContentPane().add(lblServerRunning);
		
		JLabel lblIpAddress;
		try {
			lblIpAddress = new JLabel("IP Address: " + InetAddress.getLocalHost().getHostAddress());
			lblIpAddress.setBounds(6, 46, 438, 16);
			lblIpAddress.setFocusable(true);
			lblIpAddress.addKeyListener(escapeListener);
			frame.getContentPane().add(lblIpAddress);
			
			JLabel lblPortNumber = new JLabel("Port Number: ");
			lblPortNumber.setBounds(6, 74, 126, 16);
			lblPortNumber.setFocusable(true);
			lblPortNumber.addKeyListener(escapeListener);
			frame.getContentPane().add(lblPortNumber);
			
			JButton btnClose = new JButton("Start Server");
			btnClose.setFocusable(true);
			btnClose.addKeyListener(escapeListener);
			btnClose.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					//When the startServer button is clicked, try to start the server.
					if(!serverOn) {
						try {
							port = Integer.parseInt(txtEnterPortNumber.getText().trim());
							shareDirectory = txtEnterShareDirectory.getText().trim();
							Thread server = new Thread(new TCPServer(port, shareDirectory));
							server.start();
							serverOn = true;
							lblServerRunning.setText("Server Running");
							btnClose.setText("Exit");
						} catch (Exception wrongPattern) {
							lblServerRunning.setText("Please enter a valid port number!");
						}
					}
					//After the server is started, change the button into an exit button.
					else {
						System.exit(0);
					}
				}
			});  
			btnClose.setBounds(156, 184, 161, 29);
			frame.getContentPane().add(btnClose);
			
			txtEnterPortNumber = new JTextField();
			txtEnterPortNumber.setFocusable(true);
			txtEnterPortNumber.addKeyListener(escapeListener);
			txtEnterPortNumber.setText("Enter Port Number");
			txtEnterPortNumber.setBounds(144, 68, 134, 28);
			frame.getContentPane().add(txtEnterPortNumber);
			txtEnterPortNumber.setColumns(10);
			JLabel lblShareDirectory = new JLabel("Share Directory:");
			lblShareDirectory.setFocusable(true);
			lblShareDirectory.addKeyListener(escapeListener);
			lblShareDirectory.setBounds(6, 102, 128, 16);
			frame.getContentPane().add(lblShareDirectory);
			
			txtEnterShareDirectory = new JTextField();
			txtEnterShareDirectory.setFocusable(true);
			txtEnterShareDirectory.addKeyListener(escapeListener);
			shareDirectory = System.getProperty("user.dir") + File.separator  + "share"; 
			File shares = new File(shareDirectory);
			if(!shares.exists()) 
				shares.mkdir();
			txtEnterShareDirectory.setText(shareDirectory);
			txtEnterShareDirectory.setBounds(144, 96, 312, 28);
			frame.getContentPane().add(txtEnterShareDirectory);
			txtEnterShareDirectory.setColumns(10);
			
			JLabel lblNewLabel = new JLabel("Note: Please make sure the Share Directory contains some files");
			lblNewLabel.setFocusable(true);
			lblNewLabel.addKeyListener(escapeListener);
			lblNewLabel.setBounds(6, 124, 450, 16);
			frame.getContentPane().add(lblNewLabel);
			
			JLabel lblNewLabel_1 = new JLabel("before start server. (subfolders are not seen as files) ");
			lblNewLabel_1.setFocusable(true);
			lblNewLabel_1.addKeyListener(escapeListener);
			lblNewLabel_1.setBounds(45, 145, 411, 16);
			frame.getContentPane().add(lblNewLabel_1);
			
			JLabel lblFileBeforeStart = new JLabel("");
			lblFileBeforeStart.setFocusable(true);
			lblFileBeforeStart.addKeyListener(escapeListener);
			lblFileBeforeStart.setBounds(45, 167, 272, 16);
			frame.getContentPane().add(lblFileBeforeStart);
		} catch (UnknownHostException e) {
			e.printStackTrace();
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
				toClientInfo.write(((fileNum + "\n").getBytes()));
				fromClient.readLine();
				//The server first transfers names of all the available files to the client.
				for(int i = 0; i < list.length; ++i) {
					if(list[i].isFile() && !list[i].getName().startsWith(".")) {
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
					toClientInfo.write((((Long)(file.length())).toString() + "\n").getBytes());
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
					System.out.println("noSuchFile");
					toClientInfo.write("noSuchFile\n".getBytes());
				}
			} catch (Exception e) {
				finish = true;
			} 
		}
	}
}
