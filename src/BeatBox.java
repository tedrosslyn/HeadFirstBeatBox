import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class BeatBox {

    JPanel mainPanel;
    //store the checkboxes in an arraylist
    ArrayList<JCheckBox> checkBoxArrayList;
    //need a Sequencer, Sequence and Track
    Sequencer sequencer;
    Sequence sequence;
    Track track;
    //and a Frame
    JFrame theFrame;

    //String array of instrument names for GUI labels
    String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare",
            "Crash Cymbal", "Hand Clap", "High Tom", "Hi Bongo",
            "Maracas", "Whistle", "Low Conga", "Cowbell",
            "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga"};
    //The drum channel is like a piano, except each ‘key’ on the piano is a different drum.
    //So the number ‘35’ is the key for the Bass drum, 42 is Closed Hi-Hat, etc.
    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};

    public static void main(String[] args) {
        new BeatBox().buildGUI();

    }

    private void buildGUI() {
        theFrame = new JFrame("BeatBox");
        //set default CLOSE action
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //new BorderLayout instance
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        //create a border/margin round the UI components
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //instantiate the checkBox arraylist
        checkBoxArrayList = new ArrayList<JCheckBox>();
        //box for the control buttons
        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        //create start button
        JButton start = new JButton("Start");
        //add a listener, implemented as an inner class below
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);

        //create stop button
        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);

        //create tempo up button
        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);

        //create tempo down button
        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);

        //box for the instrument labels
        Box nameBox = new Box(BoxLayout.Y_AXIS);
        //loop to add them to the box
        for (int i = 0; i < instrumentNames.length; i++) { //or hard code '16'
            nameBox.add(new Label(instrumentNames[i]));
        }

        //add the buttonBox and nameBox to the JPanel "background" EAST/WEST
        background.add(BorderLayout.WEST, nameBox);
        background.add(BorderLayout.EAST, buttonBox);
        //add the background to the JFrame "theFrame"
        theFrame.getContentPane().add(background);

        //New 16 x 16 grid for the checkboxes
        GridLayout gridLayout = new GridLayout(16, 16);
        //adjust gaps in layout
        gridLayout.setVgap(1);
        gridLayout.setHgap(2);
        //add grid to the JPanel mainPanel
        mainPanel = new JPanel(gridLayout);
        //add mainPanel to the background
        background.add(BorderLayout.CENTER, mainPanel);

        //loop through array to add checkboxes
        for (int i = 0; i < 256; i++) {
            //create a new checkbox
            JCheckBox checkBox = new JCheckBox();
            //uncheck
            checkBox.setSelected(false);
            //add it to the ArrayList
            checkBoxArrayList.add(checkBox);
            //and add to the mainPanel JPanel
            mainPanel.add(checkBox);
        }//end checkbox loop

        setUpMidi();

        //set where the frame sits on screen and its size
        theFrame.setBounds(50, 50, 300, 300);
        //Causes this Window to be sized to fit the preferred size and layouts of its subcomponents.
        theFrame.pack();
        //show frame on screen
        theFrame.setVisible(true);
    }//close buildGUI

    //set up MIDI
    public void setUpMidi() {
        //use try catch for sequencer as it can throw an exception
        try {
            //add sequencer
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            //add sequence - don't know what the parameters mean!
            sequence = new Sequence(Sequence.PPQ, 4);
            //add a track
            track = sequence.createTrack();
            //set the initial tempo
            sequencer.setTempoInBPM(120);

        } catch (Exception e) {
            e.printStackTrace();
        }
    } //close setUpMidi


    private void buildTrackAndStart() {
        //turn checkbox state into MIDI events and add them to the Track
        //Make a 16-element array to hold the values for one instrument, across all 16 beats.
        // If the instrument is supposed to play on that beat, the value at that element
        //will be the key. If that instrument is NOT supposed to play on that beat, put in a zero.
        int[] trackList = null;

        //get rid of the old track, make a fresh one.
        sequence.deleteTrack(track);
        track = sequence.createTrack();

        //do this for each of the 16 ROWS (i.e. Bass, Congo, etc.)
        for (int i = 0; i < 16; i++) {
            trackList = new int[16]; //16 rows
            //set the 'key' that represents each instrument in the instruments array
            //eg number ‘35’ is the key for the Bass drum, 42 is Closed Hi-Hat
            int key = instruments[i];

            //do this for each of the 16 BEATS in the row
            for (int j = 0; j < 16; j++) {

                JCheckBox jc = checkBoxArrayList.get(j + 16 * i);
                //the key value in this slot in the array (the slot that represents this beat).
                // Otherwise, the instrument is NOT supposed to play at this beat, so set it to zero.
                if (jc.isSelected()) {
                    trackList[j] = key;
                } else {
                    trackList[j] = 0;
                }
            }//close inner loop
            //For this instrument, and for all 16 beats, make events and add them to the track.
            makeTracks(trackList);
            track.add(makeEvent(176,1, 127, 0, 16));
        } //close outer loop

        //We always want to make sure that there IS an event at beat 16 (it goes 0 to 15).
        // Otherwise, the BeatBox might not go the full 16 beats before it starts over.
        track.add(makeEvent(192, 9, 1, 0, 15));
        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY); //or specify how many times
            sequencer.start(); //play it!
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {e.printStackTrace();}
    } //close buildTrackAndStart

      //inner class
    public class MyStartListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            buildTrackAndStart();
        }
    }

    //inner class
    public class MyStopListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            sequencer.stop();
        }
    }

    //inner class
    public class MyUpTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            //adjust up by 3%
            sequencer.setTempoFactor((float) (tempoFactor * 1.03));
        }
    }

    //inner class
    public class MyDownTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            //adjust down by 3%
            sequencer.setTempoFactor((float) (tempoFactor * 0.97));
        }
    }

    private MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
        MidiEvent event = null;
        try {
            ShortMessage shortMessage = new ShortMessage();
            shortMessage.setMessage(comd, chan ,one, two);
            event = new MidiEvent(shortMessage, tick);
        } catch(Exception e) {e.printStackTrace();}
        return event;
    }

    //This makes events for one instrument at a time, for all 16 beats.
    // So it might get an int[ ] for the Bass drum, and each index in the array will hold either
    //the key of that instrument, or a zero. If it’s a zero, the instrument isn’t supposed to play at that beat.
    //Otherwise, make an event and add it to the track
    private void makeTracks(int[] trackList) {

        for (int i =0; i < 16; i++) {
            int key = trackList[i];

            if (key != 0) { //ie it is an instrument
                track.add(makeEvent(144, 9, key, 100, i)); //NOTE ON and NOTE OFF events - add to track
                track.add(makeEvent(128, 9, key, 100, i + 1));
            }
        }
    }



}//close class
