package gbn;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.awt.event.ActionEvent;
import java.util.concurrent.Semaphore;
import javax.swing.JLabel;

public class Sender {

	private JFrame frame;
	private JTextField txtEnterReceiverIp;
	private JTextField txtEnterReceiverPort;
	private JTextField txtEnterFileName;
	protected int state = 0;
	private int blockSize = 20480;
	private int WINDOW_SIZE = 1;
	private int headerSize = 8;
	private DatagramPacket[] slidingWindow = new DatagramPacket[WINDOW_SIZE];
	private final Semaphore emptyWindow = new Semaphore(WINDOW_SIZE);
	private final Semaphore waitToBeSent = new Semaphore(0);
	private final Semaphore baseSeqNumberAccess = new Semaphore(1);
	private final Semaphore nextSeqNumberAccess = new Semaphore(1);
	private final Semaphore timerAccess = new Semaphore(1);
	private int nextSeqNumber = 0;
	private int baseSeqNumber = 0;
	private final int SEQ_NUMBER_BOUND = 64;
	private static DatagramSocket senderSocket;
	private static final int ACK_LENGTH = 8;
	private static int timeOut = 3000;
	private Timer timer;
	private SendPackets sendPackets;
	private ReceiveACK receiveACK;
	private JLabel lblNewLabel;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				
				File path = new File("send" + File.separator);
				if(!path.exists()) 
					path.mkdir();
				
				try {
					Sender window = new Sender();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Sender() {
		initialize();
		startProcess();
	}
	
	private void startProcess() {
		try {
			senderSocket = new DatagramSocket(2376);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sendPackets = new SendPackets(senderSocket);
		receiveACK = new ReceiveACK(senderSocket);
		sendPackets.start();
		receiveACK.start();
		timer = new Timer(timeOut, sendPackets);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		txtEnterReceiverIp = new JTextField();
		txtEnterReceiverIp.setText("Enter Receiver IP");
		txtEnterReceiverIp.setBounds(64, 46, 134, 28);
		frame.getContentPane().add(txtEnterReceiverIp);
		txtEnterReceiverIp.setColumns(10);
		
		txtEnterReceiverPort = new JTextField();
		txtEnterReceiverPort.setText("Enter Receiver Port");
		txtEnterReceiverPort.setBounds(226, 46, 134, 28);
		frame.getContentPane().add(txtEnterReceiverPort);
		txtEnterReceiverPort.setColumns(10);
		
		txtEnterFileName = new JTextField();
		txtEnterFileName.setText("Enter File Name");
		txtEnterFileName.setBounds(160, 86, 134, 28);
		frame.getContentPane().add(txtEnterFileName);
		txtEnterFileName.setColumns(10);
		
		JButton btnSend = new JButton("Send");
		btnSend.setBounds(171, 167, 117, 29);
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String fileName = txtEnterFileName.getText().trim();
				File file = new File("send" + File.separator + fileName);
				lblNewLabel.setText("Start sending at " + System.currentTimeMillis() + "ms");
				try {
					sendFile(file);
				} catch (IOException | InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		frame.getContentPane().add(btnSend);
		
		JButton btnExit = new JButton("Exit");
		btnExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				senderSocket.close();
				System.exit(0);
			}
		});
		btnExit.setBounds(171, 218, 117, 29);
		frame.getContentPane().add(btnExit);
		
		lblNewLabel = new JLabel("");
		lblNewLabel.setBounds(6, 147, 426, 16);
		frame.getContentPane().add(lblNewLabel);
	}
	
	private void sendFile(File file) throws IOException, InterruptedException {
		nextSeqNumberAccess.acquire();
		baseSeqNumberAccess.acquire();
		nextSeqNumber = 0;
		baseSeqNumber = 0;
		nextSeqNumberAccess.release();
		baseSeqNumberAccess.release();
		BufferedInputStream fileInput = new BufferedInputStream(new FileInputStream(file));
		Integer len = 0;
		Long unreadSize = file.length();
		sendFileInfo(file.getName().split(File.separator)[file.getName().split(File.separator).length - 1]);
		sendFileInfo(unreadSize.toString());
		Long sizeToRead = (blockSize < unreadSize ? blockSize : unreadSize);
		byte[] buffer = new byte[sizeToRead.intValue() + headerSize];
		while(unreadSize > 0 && (len = fileInput.read(buffer, headerSize, sizeToRead.intValue())) != -1) {
			unreadSize -= len;
			rdtSend(buffer, sizeToRead.intValue());
			sizeToRead = (blockSize < unreadSize ? blockSize : unreadSize);
			buffer = new byte[sizeToRead.intValue() + headerSize];
		}
		fileInput.close();
	}
	
	private void sendFileInfo(String fileInfo) throws InterruptedException, IOException {
		byte[] nameBytes = fileInfo.getBytes();
		InetAddress IPAddress = InetAddress.getByName(txtEnterReceiverIp.getText());
		int port = Integer.parseInt(txtEnterReceiverPort.getText());
		DatagramPacket p = new DatagramPacket(nameBytes, 0, nameBytes.length, IPAddress, port);
		senderSocket.send(p);
	}
	
	private void rdtSend(byte[] buffer, int len) throws InterruptedException, UnknownHostException {
		//System.out.println(len + headerSize);
		int seqNumber = addHeader(buffer, len);
		//System.out.println(seqNumber);
		InetAddress IPAddress = InetAddress.getByName(txtEnterReceiverIp.getText());
		int port = Integer.parseInt(txtEnterReceiverPort.getText());
		DatagramPacket p = new DatagramPacket(buffer, 0, len + headerSize, IPAddress, port);
		baseSeqNumberAccess.acquire();
		slidingWindow[seqNumber % WINDOW_SIZE] = p;
		System.out.println(seqNumber % WINDOW_SIZE);
		if(baseSeqNumber == seqNumber) {
			timerAccess.acquire();
			if(!timer.isAlive())
				timer.start();
			else 
				timer.interrupt();
			timerAccess.release();
		}
		sendPackets.send(seqNumber % WINDOW_SIZE);
		baseSeqNumberAccess.release();
		waitToBeSent.release();
	}
	
	private int addHeader(byte[] buffer, int len) throws InterruptedException {
		addCRC(buffer, len);
		return addSeqNumber(buffer);
	}	
	
	private int addSeqNumber(byte[] buffer) throws InterruptedException {
		int seqNumber = waitForAvailableSeqNumber();
		System.out.println("Adding: " + seqNumber);
		for(int i = 0; i < 4; ++i) {
			buffer[i] = (byte)((seqNumber >> (i * 8)) & 0x000000ff);
		}
		return seqNumber;
	}
	
	private void addCRC(byte[] buffer, int len) {
		//TODO
	}
	
	private int waitForAvailableSeqNumber() throws InterruptedException {
		//System.out.println("before " + emptyWindow.availablePermits());
		emptyWindow.acquire();
		//System.out.println("after " + emptyWindow.availablePermits());
		nextSeqNumberAccess.acquire();
		int seqNumber = nextSeqNumber;
		nextSeqNumber = (nextSeqNumber + 1) % SEQ_NUMBER_BOUND;
		nextSeqNumberAccess.release();
		return seqNumber;
	}
	
	private class Timer extends Thread {
		private int time;
		private SendPackets sendPackets;
		private Boolean kill = false;
		
		Timer(int time, SendPackets sendPackets) {
			this.time = time;
			this.sendPackets = sendPackets;
		}
		
		public void kill() {
			kill = true;
		}
		
		public void run() {
			while(!kill) {
				try {
					sleep(time);
					sendPackets.reSend();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
			}
		}
	}
	
	private class SendPackets extends Thread {
		
		private DatagramSocket senderSocket;
		private int nextToSend = 0;
		
		SendPackets(DatagramSocket senderSocket) {
			this.senderSocket = senderSocket;
		}
		
		public void send(int nextToSend) {
			this.nextToSend = nextToSend;
		}
		
		public void run() {
			while(true) {
				try {
					waitToBeSent.acquire();
					//System.out.println("slidingWindow[" + nextToSend + "] = null ? " + slidingWindow[nextToSend] == null);
					senderSocket.send(slidingWindow[nextToSend]);
				} catch (InterruptedException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		public void reSend() {
			System.out.println("resend");
			try {
				baseSeqNumberAccess.acquire();
				nextSeqNumberAccess.acquire();
				for(int i = baseSeqNumber; i != nextSeqNumber; i = (i + 1) % SEQ_NUMBER_BOUND) {
					//System.out.println("resending " + i);
					senderSocket.send(slidingWindow[i % WINDOW_SIZE]);
				}
				nextSeqNumberAccess.release();
				baseSeqNumberAccess.release();
			} catch (InterruptedException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private class ReceiveACK extends Thread {
		
		private DatagramSocket senderSocket;
		
		ReceiveACK(DatagramSocket senderSocket) {
			this.senderSocket = senderSocket;
		}
		
		public void run() {
			while(true) {
				byte[] ACK = new byte[ACK_LENGTH];
				DatagramPacket ACKPacket = new DatagramPacket(ACK, ACK_LENGTH);
				try {
					senderSocket.receive(ACKPacket);
					if(!corrupted(ACK)) {
						int seqNumber = extractSeqNumber(ACK);
						baseSeqNumberAccess.acquire();
						nextSeqNumberAccess.acquire();
						//System.out.println("yes 1");
						//System.out.println(seqNumber);
						if((seqNumber - baseSeqNumber + SEQ_NUMBER_BOUND) % SEQ_NUMBER_BOUND <= 
								(nextSeqNumber - baseSeqNumber + SEQ_NUMBER_BOUND) % SEQ_NUMBER_BOUND) {
							//System.out.println("no 1");
							int oldBase = baseSeqNumber;
							baseSeqNumber = (seqNumber + 1) % SEQ_NUMBER_BOUND;
							for(int i = oldBase; i != baseSeqNumber; i = (i + 1) % SEQ_NUMBER_BOUND) {
								emptyWindow.release();
								//System.out.println(emptyWindow.availablePermits());
							}
							if(baseSeqNumber == nextSeqNumber) {
								//System.out.println("finished");
								timerAccess.acquire();
								if(timer.isAlive()) {
									timer.kill();
									timer.interrupt();
									timer = new Timer(timeOut, sendPackets);
									timerAccess.release();
								}
							}
							else {
								timerAccess.acquire();
								if(!timer.isAlive())
									timer.start();
								else 
									timer.interrupt();
								timerAccess.release();
							}
						}
						nextSeqNumberAccess.release();
						baseSeqNumberAccess.release();
					}
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		private Boolean corrupted(byte[] ACK) {
			//TODO
			return false;
		}
		
		private int extractSeqNumber(byte[] ACK) {
			int seqNumber = 0;
			for(int i = 0; i < 4; ++i) {
				seqNumber |= (ACK[i] << (i * 8));
			}
			return seqNumber;
		}
	}
}
