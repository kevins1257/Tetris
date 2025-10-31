import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Random;

/**
 * A clean, playable Tetris game using Java Swing.
 * Written for a college CS project – readable, maintainable, and fun.
 *
 * @author Your Name
 * @version 1.0
 */
public class TetrisGame extends JPanel {

    // ==================== CONSTANTS ====================
    private static final int ROWS = 20;
    private static final int COLS = 10;
    private static final int BLOCK_SIZE = 30;
    private static final int INITIAL_DELAY = 500; // ms per drop

    // Tetromino colors (index 0 = empty)
    private static final Color[] COLORS = {
            Color.BLACK,
            Color.CYAN, // I
            Color.BLUE, // J
            Color.ORANGE, // L
            Color.YELLOW, // O
            Color.GREEN, // S
            Color.MAGENTA, // T
            Color.RED // Z
    };

    // All 7 tetromino shapes (4 rotations not precomputed – we rotate on fly)
    private static final int[][][] SHAPES = {
            { { 1, 1, 1, 1 } }, // I
            { { 1, 1 }, { 1, 1 } }, // O
            { { 0, 1, 0 }, { 1, 1, 1 } }, // T
            { { 0, 1, 1 }, { 1, 1, 0 } }, // S
            { { 1, 1, 0 }, { 0, 1, 1 } }, // Z
            { { 1, 0, 0 }, { 1, 1, 1 } }, // J
            { { 0, 0, 1 }, { 1, 1, 1 } } // L
    };

    // ==================== GAME STATE ====================
    private final int[][] board = new int[ROWS][COLS];
    private Tetromino currentPiece;
    private int score = 0;
    private JLabel scoreLabel;
    private Timer dropTimer;

    // ==================== CONSTRUCTOR ====================
    public TetrisGame() {
        setupPanel();
        setupScoreLabel();
        setupTimer();
        setupKeyListener();
        spawnNewPiece();
        dropTimer.start();
    }

    // ==================== SETUP METHODS ====================
    private void setupPanel() {
        setPreferredSize(new Dimension(COLS * BLOCK_SIZE + 200, ROWS * BLOCK_SIZE));
        setBackground(Color.DARK_GRAY);
        setFocusable(true);
        setLayout(null); // We'll position score label manually
    }

    private void setupScoreLabel() {
        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setFont(new Font("Consolas", Font.BOLD, 18));
        scoreLabel.setBounds(COLS * BLOCK_SIZE + 20, 20, 150, 30);
        add(scoreLabel);
    }

    private void setupTimer() {
        dropTimer = new Timer(INITIAL_DELAY, (ActionEvent e) -> dropOneRow());
        dropTimer.setDelay(INITIAL_DELAY);
    }

    private void setupKeyListener() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT -> move(-1);
                    case KeyEvent.VK_RIGHT -> move(1);
                    case KeyEvent.VK_DOWN -> dropOneRow();
                    case KeyEvent.VK_UP -> rotate();
                    case KeyEvent.VK_SPACE -> hardDrop();
                }
                repaint();
            }
        });
    }

    // ==================== GAME LOGIC ====================
    private void spawnNewPiece() {
        Random rand = new Random();
        int type = rand.nextInt(SHAPES.length);
        int[][] shape = deepCopy(SHAPES[type]);
        int startX = COLS / 2 - shape[0].length / 2;

        currentPiece = new Tetromino(shape, startX, 0, type + 1);

        if (!isValidPosition(currentPiece)) {
            dropTimer.stop();
            JOptionPane.showMessageDialog(this, "Game Over!\nScore: " + score,
                    "Tetris", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
    }

    private void dropOneRow() {
        Tetromino test = currentPiece.clone();
        test.y++;
        if (isValidPosition(test)) {
            currentPiece.y++;
        } else {
            mergePiece();
            clearFullLines();
            spawnNewPiece();
        }
        repaint();
    }

    private void move(int dx) {
        Tetromino test = currentPiece.clone();
        test.x += dx;
        if (isValidPosition(test))
            currentPiece.x += dx;
    }

    private void rotate() {
        int[][] rotated = rotate90(currentPiece.shape);
        Tetromino test = currentPiece.clone();
        test.shape = rotated;
        if (isValidPosition(test))
            currentPiece.shape = rotated;
    }

    private void hardDrop() {
        while (true) {
            Tetromino test = currentPiece.clone();
            test.y++;
            if (!isValidPosition(test))
                break;
            currentPiece.y++;
            score += 2;
        }
        updateScore();
        mergePiece();
        clearFullLines();
        spawnNewPiece();
        repaint();
    }

    private void mergePiece() {
        for (int r = 0; r < currentPiece.shape.length; r++) {
            for (int c = 0; c < currentPiece.shape[0].length; c++) {
                if (currentPiece.shape[r][c] == 1) {
                    int boardY = currentPiece.y + r;
                    int boardX = currentPiece.x + c;
                    if (boardY >= 0 && boardY < ROWS && boardX >= 0 && boardX < COLS) {
                        board[boardY][boardX] = currentPiece.type;
                    }
                }
            }
        }
    }

    private void clearFullLines() {
        int linesCleared = 0;
        for (int r = ROWS - 1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < COLS; c++) {
                if (board[r][c] == 0) {
                    full = false;
                    break;
                }
            }
            if (full) {
                // Shift all rows above down
                for (int above = r; above > 0; above--) {
                    System.arraycopy(board[above - 1], 0, board[above], 0, COLS);
                }
                Arrays.fill(board[0], 0); // Now works!
                linesCleared++;
                r++; // recheck current row after shift
            }
        }

        if (linesCleared > 0) {
            score += (int) Math.pow(2, linesCleared) * 100;
            updateScore();
            dropTimer.setDelay(Math.max(100, INITIAL_DELAY - score / 10));
        }
    }

    private void updateScore() {
        scoreLabel.setText("Score: " + score);
    }

    // ==================== VALIDATION & UTILS ====================
    private boolean isValidPosition(Tetromino p) {
        for (int r = 0; r < p.shape.length; r++) {
            for (int c = 0; c < p.shape[0].length; c++) {
                if (p.shape[r][c] == 1) {
                    int boardY = p.y + r;
                    int boardX = p.x + c;
                    if (boardX < 0 || boardX >= COLS || boardY >= ROWS)
                        return false;
                    if (boardY >= 0 && board[boardY][boardX] != 0)
                        return false;
                }
            }
        }
        return true;
    }

    private int[][] rotate90(int[][] original) {
        int rows = original.length;
        int cols = original[0].length;
        int[][] rotated = new int[cols][rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                rotated[c][rows - 1 - r] = original[r][c];
            }
        }
        return rotated;
    }

    private int[][] deepCopy(int[][] original) {
        int[][] copy = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = original[i].clone();
        }
        return copy;
    }

    // ==================== PAINTING ====================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw board
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int value = board[r][c];
                if (value != 0) {
                    g.setColor(COLORS[value]);
                    g.fillRect(c * BLOCK_SIZE, r * BLOCK_SIZE, BLOCK_SIZE - 1, BLOCK_SIZE - 1);
                }
                g.setColor(Color.GRAY);
                g.drawRect(c * BLOCK_SIZE, r * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
            }
        }

        // Draw current falling piece
        if (currentPiece != null) {
            for (int r = 0; r < currentPiece.shape.length; r++) {
                for (int c = 0; c < currentPiece.shape[0].length; c++) {
                    if (currentPiece.shape[r][c] == 1) {
                        int x = (currentPiece.x + c) * BLOCK_SIZE;
                        int y = (currentPiece.y + r) * BLOCK_SIZE;
                        if (y >= 0) {
                            g.setColor(COLORS[currentPiece.type]);
                            g.fillRect(x, y, BLOCK_SIZE - 1, BLOCK_SIZE - 1);
                        }
                    }
                }
            }
        }
    }

    // ==================== MAIN ====================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Tetris – CS Project");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            TetrisGame game = new TetrisGame();
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            game.requestFocusInWindow();
        });
    }
}

/**
 * Simple immutable tetromino representation.
 * Uses deep clone to avoid reference bugs.
 */
class Tetromino implements Cloneable {
    int[][] shape;
    int x, y, type;

    Tetromino(int[][] shape, int x, int y, int type) {
        this.shape = shape;
        this.x = x;
        this.y = y;
        this.type = type;
    }

    @Override
    public Tetromino clone() {
        int[][] copy = new int[shape.length][];
        for (int i = 0; i < shape.length; i++) {
            copy[i] = shape[i].clone();
        }
        return new Tetromino(copy, x, y, type);
    }
}
