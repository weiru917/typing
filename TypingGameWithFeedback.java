import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class TypingGameWithFeedback {
    private static final int TOTAL_WORDS = 10;
    private static final int INITIAL_TIME = 10;

    private JLabel timerLabel;
    private JTextPane wordPane;
    private JTextField inputField;

    private Timer timer;
    private int remainingTime;
    private String[] printedWords;
    private boolean gameStarted;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TypingGameWithFeedback game = new TypingGameWithFeedback();
            game.createAndShowGUI();
        });
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Type-A-Thon");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        try {
            // Load the image for the icon
            Image iconImage = new ImageIcon(new File("src/typing.png").toURI().toURL()).getImage();

            // Set the icon image for the frame
            frame.setIconImage(iconImage);
        } catch (IOException e) {
            e.printStackTrace();
        }

        timerLabel = new JLabel("Press any key to start", SwingConstants.CENTER);
        timerLabel.setFont(timerLabel.getFont().deriveFont(16.0f));
        frame.add(timerLabel, BorderLayout.NORTH);

        wordPane = new JTextPane();
        wordPane.setEditable(false);
        wordPane.setFont(wordPane.getFont().deriveFont(16.0f));
        frame.add(new JScrollPane(wordPane), BorderLayout.CENTER);

        inputField = new JTextField();
        inputField.setFont(inputField.getFont().deriveFont(16.0f));
        inputField.getDocument().addDocumentListener(new InputDocumentListener());
        frame.add(inputField, BorderLayout.SOUTH);

        frame.setVisible(true);

        // Load words, but don't start the timer immediately
        loadWords();
    }

    private void startGame() {
        remainingTime = INITIAL_TIME;
        updateTimerLabel();

        timer = new Timer(1000, new TimerActionListener());
        timer.start();

        inputField.requestFocus();
    }

    private void loadWords() {
        Random random = new Random();
        List<String> words = readWordsFromFile();

        printedWords = new String[TOTAL_WORDS];
        StringBuilder wordDisplay = new StringBuilder();

        for (int i = 0; i < TOTAL_WORDS; i++) {
            String randomWord = words.get(random.nextInt(words.size()));
            printedWords[i] = randomWord;
            wordDisplay.append(randomWord).append(" ");
        }

        showWordsWithColor(wordDisplay.toString(), Color.DARK_GRAY);
    }

    private List<String> readWordsFromFile() {
        List<String> words = new java.util.ArrayList<>();
        try (Scanner fileSc = new Scanner(new File("src/word.txt"))) {
            while (fileSc.hasNextLine()) {
                words.add(fileSc.nextLine());
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return words;
    }

    private void updateTimerLabel() {
        timerLabel.setText("Time remaining: " + remainingTime + " seconds");
    }

    private void showWordsWithColor(String text, Color color) {
        StyledDocument doc = wordPane.getStyledDocument();
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, color);
        doc.setCharacterAttributes(0, doc.getLength(), set, true);
        wordPane.setText(text);
    }

    private void updateColorsForTypedText(String typedText) {
        StyledDocument doc = wordPane.getStyledDocument();
        int cursorPosition = 0;
        int charIndex = 0;

        for (int i = 0; i < printedWords.length; i++) {
            String printedWord = printedWords[i];
            SimpleAttributeSet originalSet = new SimpleAttributeSet();
            StyleConstants.setForeground(originalSet, Color.DARK_GRAY);

            for (int j = 0; j < printedWord.length(); j++) {
                SimpleAttributeSet set = new SimpleAttributeSet();
                if (charIndex < typedText.length()) {
                    if (typedText.charAt(charIndex) == printedWord.charAt(j)) {
                        StyleConstants.setForeground(set, Color.GREEN);
                    } else {
                        StyleConstants.setForeground(set, Color.RED);
                    }
                    charIndex++;
                } else {
                    // If there are remaining characters in printedWord, set their color to DARK_GRAY
                    StyleConstants.setForeground(set, Color.DARK_GRAY);
                }

                doc.setCharacterAttributes(cursorPosition, 1, set, false);
                cursorPosition++;
            }

            // Add space between words (except for the last word)
            if (i < printedWords.length - 1) {
                // Check for space after the typed text
                if (charIndex < typedText.length() && typedText.charAt(charIndex) == ' ') {
                    StyleConstants.setForeground(originalSet, Color.GREEN); // Set space color to GREEN
                    charIndex++;
                } else {
                    StyleConstants.setForeground(originalSet, Color.DARK_GRAY);
                }
                doc.setCharacterAttributes(cursorPosition, 1, originalSet, false);
                cursorPosition++;
            }
        }
    }

    private class TimerActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (remainingTime > 0) {
                remainingTime--;
                updateTimerLabel();
            } else {
                timer.stop();
                inputField.setEnabled(false);

                String[] userType = inputField.getText().split(" ");
                accuracyAndWPM(userType);

                // Clear input field and word area for the next game
                inputField.setText("");
                wordPane.setText("");

                // Start a new game
                gameStarted = false;
                inputField.setEnabled(true);
                loadWords();
                // Only start the game when the user types the first character
            }
        }
    }

    private void accuracyAndWPM(String[] userType) {
        StyledDocument doc = wordPane.getStyledDocument();
        int correctCharacters = 0;
        int totalCharacters = 0;
        int cursorPosition = 0;

        for (int i = 0; i < Math.min(userType.length, printedWords.length); i++) {
            String userWord = userType[i];
            String printedWord = printedWords[i];

            int minLength = Math.min(userWord.length(), printedWord.length());

            for (int j = 0; j < minLength; j++) {
                totalCharacters++;
                if (userWord.charAt(j) == printedWord.charAt(j)) {
                    correctCharacters++;
                }
            }

            // Add space between words (except for the last word)
            totalCharacters += (userWord.length() < printedWord.length()) ? 1 : 0;
        }

        double accuracy = ((double) correctCharacters / totalCharacters) * 100;
        double wpm = (totalCharacters / 5.0) / (INITIAL_TIME / 60.0);

        JOptionPane.showMessageDialog(null,
                String.format("Game Over!\nAccuracy: %.2f%%\nWPM: %.2f", accuracy, wpm),
                "Game Over", JOptionPane.INFORMATION_MESSAGE);
    }

    private class InputDocumentListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            handleTextChange();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            handleTextChange();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            // Plain text components don't fire these events
        }

        private void handleTextChange() {
            if (!gameStarted) {
                gameStarted = true;
                startGame(); // Start the game when the user types the first character
            }

            // Get the current typed text
            String typedText = inputField.getText();
            updateColorsForTypedText(typedText);
        }
    }
}