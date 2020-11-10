import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

public class SearchTool {
	
	private static final String PRODUCT_NAME = "JS Search Tool";
	private static final String PRODUCT_VERSION = "1.0.2";
	
	final JFileChooser fc = new JFileChooser();
	final ImageIcon icon = new ImageIcon(ResourceLoader.getImage("js.png"));
	final ImageIcon smallIcon = new ImageIcon(ResourceLoader.getImage("js_small.png"));
	
	JMenuBar menuBar;
	JMenu menu, help;
	JMenuItem menuItemSave, menuItemLoad, menuItemAbout;
	
	JTextField srcText, keyText, startText, endText, destText;
	JButton startBtn, cancelBtn;
	JLabel srcLbl, keyLbl, startLbl, endLbl, destLbl, infoLbl, copyright;
	JProgressBar bar;
	JFrame frame;
	SwingWorker<Void, Void> worker;
	
	private void searchFiles(File src, String pattern, long startTick, long endTick, File dest) 
				throws FileNotFoundException, InterruptedException {		
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date1 = new Date();
		    
		// get list of files in given directory
		File[] files = src.listFiles();
		int size = files.length;
		
		File file;
		long lastModified;
		int count = 0;
		bar.setVisible(true);
		
		// loop over list of files
		for (int i = 0; i < size; i++) {
			
			if (worker.isCancelled() == true) {
				throw new InterruptedException("");
			}
			
			file = files[i];
			
			infoLbl.setText("Processing file " + file.getName());
			bar.setValue((i + 1) * 100 / size);
			
			lastModified = file.lastModified();
			
			if (file.isFile() && lastModified < endTick && lastModified >= startTick) {

				Reader reader = new InputStreamReader(new FileInputStream(file));
					BufferedReader reader1 = new BufferedReader(reader);
				try {
					String line = reader1.readLine();
					while(line != null) {

						// Match found
						if (line.contains(pattern)) {
							count++;
							System.out.println("Match found in file " + file.getName());

						try {
							Files.copy(file.toPath(), (new File(dest.getAbsolutePath() + "\\" + file.getName())).toPath(), 
											StandardCopyOption.REPLACE_EXISTING);
							} catch (IOException e) {
								System.out.println("Failed to copy file " + file.getName() + " to " + dest);
							}
							break;
						}

						line = reader1.readLine();
					}
					reader.close();
					reader1.close();
				} catch (IOException e) {
					System.out.println("Failed to read file " + file.getName());
				}
			}
		}

		Date date2 = new Date();
		System.out.println();
		System.out.println("Start time " + dateFormat.format(date1));
		System.out.println("End time " + dateFormat.format(date2));

		infoLbl.setText("Done");
		JOptionPane.showMessageDialog(frame, "Done! Total: " + count + " file(s) matched");
		
	}
	
	private void buttonPressed() throws InterruptedException {
		
		// validate input
		
		File src = new File(srcText.getText());		
		if (!src.isDirectory()) {
			JOptionPane.showMessageDialog(frame, "Source has to be a directory");
				throw new IllegalArgumentException("Source has to be a directory");
		}
	    
		String pattern = keyText.getText();  
		if (pattern.isEmpty()) {
			JOptionPane.showMessageDialog(frame, "Keyword cannot empty");
				throw new IllegalArgumentException("Keyword cannot empty");
		}
		
		String startDateString = startText.getText();
		String endDateString = endText.getText();
		
		if (startDateString.isEmpty() || endDateString.isEmpty()) {
			JOptionPane.showMessageDialog(frame, "Date cannot empty");
				throw new IllegalArgumentException("Date cannot be empty");
		}
		
		File dest = new File(destText.getText());
		if (!dest.isDirectory()) {
			JOptionPane.showMessageDialog(frame, "Destination has to be a directory");
			throw new IllegalArgumentException("Destination has to be a directory");
		}
		
		try {
			SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd");
			Date startDate = f.parse(startDateString);
			long startTick = startDate.getTime();

			Date endDate = f.parse(endDateString);
			long endTick = endDate.getTime();

			cancelBtn.setEnabled(true);
			toggleInput(false);
			infoLbl.setText("Preparing...");
    		
			// call method
			searchFiles(src, pattern, startTick, endTick, dest);
			
		} catch(FileNotFoundException e) {
			System.out.println("FileNotFound");			
		} catch(ParseException e) {
			System.out.println("ParseException: Date entered is not valid");
			JOptionPane.showMessageDialog(frame, "Date entered is not valid");			
		}
	}
	
	private void toggleInput(boolean onOff) {
		srcText.setEnabled(onOff);
		keyText.setEnabled(onOff);
		startText.setEnabled(onOff);
		endText.setEnabled(onOff);
		destText.setEnabled(onOff);
		menu.setEnabled(onOff);
	}
	
	private void openConfigFile(File file) {        
		System.out.println("Reading input from config: " + file.getName());
		try {
			Scanner scanner = new Scanner(file);
			String s1 = scanner.nextLine();
			String s2 = scanner.nextLine();
			String s3 = scanner.nextLine();
			String s4 = scanner.nextLine();
			String s5 = scanner.nextLine();
			
			srcText.setText(s1);
			keyText.setText(s2);
			startText.setText(s3);
			endText.setText(s4);
			destText.setText(s5);
			
			scanner.close();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(frame, "Invalid input file");
		}
	}
	
	private void saveConfigFile(File file) {
		System.out.println("Saving config to: " + file.getName());
		try {
			PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
			writer.println(srcText.getText());
			writer.println(keyText.getText());
			writer.println(startText.getText());
			writer.println(endText.getText());
			writer.println(destText.getText());

			writer.close();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(frame, "Failed to save file");
		}
	}
	
	SearchTool() {
		
		// initialize components
		
		// menu bar
		menuBar = new JMenuBar();
		
		menu = new JMenu("Menu    ");
		menu.setMnemonic(KeyEvent.VK_ALT);
		menuBar.add(menu);
		menuItemSave = new JMenuItem("Save");
		menuItemSave.setAccelerator(KeyStroke.getKeyStroke(
		KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		menu.add(menuItemSave);
		menuItemSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Save config
				int returnVal = fc.showSaveDialog(frame);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					saveConfigFile(file);
				} else {
					System.out.println("Open command cancelled by user.");
				}
			}
		});
		menuItemLoad = new JMenuItem("Open");
		menuItemLoad.setAccelerator(KeyStroke.getKeyStroke(
		KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		menu.add(menuItemLoad);		
		menuItemLoad.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int returnVal = fc.showOpenDialog(frame);
				
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				openConfigFile(file);
			} else {
				System.out.println("Open command cancelled by user.");
			}
		    }
		});

		help = new JMenu("Help    ");
		menuBar.add(help);
		menuItemAbout = new JMenuItem("About");
		help.add(menuItemAbout);
		menuItemAbout.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {				

				JOptionPane.showMessageDialog(null, 
					PRODUCT_NAME + "\nVersion: " + PRODUCT_VERSION + "\n\nCopyright (c) 2018 \nAll Rights Reserved."
					, "About Search Tool", JOptionPane.PLAIN_MESSAGE, smallIcon);
			}
		});		
		
		
		// labels
		srcLbl = new JLabel("Search directory");
		srcLbl.setBounds(10, 10, 200, 20);	
		keyLbl = new JLabel("Search keyword");
		keyLbl.setBounds(10, 60, 200, 20);
		startLbl = new JLabel("Start date - inclusive (yyyy/MM/dd)");
		startLbl.setBounds(10, 110, 200, 20);
		endLbl = new JLabel("End date - exclusive (yyyy/MM/dd)");
		endLbl.setBounds(220, 110, 200, 20);	
		destLbl = new JLabel("Copy matched files to");
		destLbl.setBounds(10, 160, 200, 20);
		infoLbl = new JLabel();
		infoLbl.setBounds(10, 270, 410, 20);
		
		copyright = new JLabel("<html>Copyright (c) 2018 All Rights Reserved.</html>");
		copyright.setBounds(280, 220, 140, 30);
		copyright.setFont(new Font(copyright.getName(), Font.PLAIN, 9));
	
		// text fields
		srcText = new JTextField();
		srcText.setBounds(10, 30, 400, 20);		
		keyText = new JTextField();
		keyText.setBounds(10, 80, 200, 20);	
		startText = new JTextField();
		startText.setBounds(10, 130, 150, 20);
		endText = new JTextField();
		endText.setBounds(220, 130, 150, 20);
		destText = new JTextField();
		destText.setBounds(10, 180, 400, 20);	
		
		// progress bar
		bar = new JProgressBar();
		bar.setBounds(10, 290, 410, 15);
		bar.setStringPainted(true);
		bar.setVisible(false);
		
		// start button
		startBtn = new JButton("Start");
		startBtn.setBounds(10, 220, 100, 20);
		startBtn.addActionListener(new ActionListener() {			
			public void actionPerformed(ActionEvent e) {
                		worker = new SwingWorker<Void, Void>() {
					@Override
					protected Void doInBackground() {
					startBtn.setEnabled(false);
					try {
						buttonPressed();
					} catch (InterruptedException e) {
						infoLbl.setText("Canceled");
						bar.setValue(0);;
					}
					return null;
					}                    
					@Override
					protected void done() {
						startBtn.setEnabled(true);
						cancelBtn.setEnabled(false);
						toggleInput(true);
					}

				};                
                		worker.execute();
			}
		} );
		
		// cancel button
		cancelBtn = new JButton("Cancel");
		cancelBtn.setBounds(130, 220, 100, 20);
		cancelBtn.setEnabled(false);
		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {		    	
				if (worker != null) {
					// Stop the swing worker thread
					worker.cancel(true);
				}
			}
		});
		
		// initialize frame
		frame = new JFrame();
		frame.setLocation(200, 200);
		frame.setSize(450, 380);
		frame.setLayout(null);
		frame.setVisible(true);
		frame.setTitle(PRODUCT_NAME + " v" + PRODUCT_VERSION);
		frame.setJMenuBar(menuBar);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		
		// add components to frame
		frame.add(srcLbl);
		frame.add(keyLbl);
		frame.add(startLbl);
		frame.add(endLbl);
		frame.add(destLbl);
		frame.add(srcText);
		frame.add(keyText);
		frame.add(startText);
		frame.add(endText);
		frame.add(destText);
		frame.add(startBtn);
		frame.add(cancelBtn);
		frame.add(infoLbl);
		frame.add(bar);
		frame.add(copyright);
		
		frame.setIconImage(icon.getImage());
		
	}
	
	public static void main(String[] args) {		
		// Initialize
		new SearchTool();
	}

}
