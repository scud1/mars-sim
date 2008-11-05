/**
 * Mars Simulation Project
 * AudioPlayer.java
 * @version 2.83 2008-03-01
 * @author Dima Stepanchuk
 * @author Sebastien Venot
 */

package org.mars_sim.msp.ui.standard.sound;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;

import org.mars_sim.msp.ui.standard.UIConfig;

/**
 * A class to play sound files.
 */
public class AudioPlayer implements LineListener, MetaEventListener {

    private static String CLASS_NAME = "org.mars_sim.msp.ui.standard.sound.AudioPlayer";

    private static Logger logger = Logger.getLogger(CLASS_NAME);

    // Data members
    /** The current compressed sound. */
    private SourceDataLine currentLine;

    /** The current clip sound. */
    private Clip currentClip;

    /** The current midi sound. */
    private Sequencer sequencer;

    /** midi sound synthetiser */
    private Synthesizer synthesizer;

    /** midi sound receiver */
    private Receiver synthReceiver;

    /** midi sound transmitter */
    private Transmitter seqTransmitter;

    /** Is the audio player muted? */
    private boolean mute;

    /** The volume of the audio player (0.0 to 1.0) */
    private float volume;

    /** Audio cache collection */
    private ConcurrentHashMap<String, Clip> audioCache = new ConcurrentHashMap<String, Clip>();

    /** looping mode */
    private boolean looping = false;

    /** sound player thread instance */
    private Thread sound_player;

    public AudioPlayer() {
        currentClip = null;
        currentLine = null;
        sequencer = null;
        synthesizer = null;
        synthReceiver = null;
        seqTransmitter = null;

        if (UIConfig.INSTANCE.useUIDefault()) {
            setMute(false);
            setVolume(.5F);
        } else {
            setMute(UIConfig.INSTANCE.isMute());
            setVolume(UIConfig.INSTANCE.getVolume());
        }
    }

    /**
     * Starts playing a sound (either compressed or not)
     * 
     * @param filepath the file path to the sound .
     * @param loop Should the sound clip be looped?
     */
    private void startPlay(final String filepath, final boolean loop) {

        // if the sound is long(the whole UI get stuck, so we play
        // the sound within his own thread
        sound_player = new Thread() {
            public void run() {
                if ((filepath != null) && !filepath.equals("")) {
                    if (filepath.endsWith(SoundConstants.SND_FORMAT_WAV)) {
                        startPlayWavSound(filepath, loop);
                    } else if (filepath.endsWith(SoundConstants.SND_FORMAT_MP3) || filepath.endsWith(SoundConstants.SND_FORMAT_OGG)) {
                        startPlayCompressedSound(filepath, loop);
                    } else if (filepath.endsWith(SoundConstants.SND_FORMAT_MIDI) || filepath.endsWith(SoundConstants.SND_FORMAT_MID)) {
                        startMidiSound(filepath, loop);
                    }
                }
            }

        };

        sound_player.setPriority(Thread.MIN_PRIORITY);
        sound_player.setName(filepath);
        sound_player.setDaemon(true);
        sound_player.start();
    }

    /**
     * Play and cache sound of type wav.
     * 
     * @param filepath the file path to the sound
     * @param loop Should the sound clip be looped?
     */
    public void startPlayWavSound(String filepath, boolean loop) {
        try {
            if (!audioCache.containsKey(filepath)) {
                File soundFile = new File(filepath);
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
                currentClip = AudioSystem.getClip();
                currentClip.open(audioInputStream);
                audioCache.put(filepath, currentClip);

                if (audioCache.size() > SoundConstants.MAX_CACHE_SIZE) {
                    Object[] keys = audioCache.keySet().toArray();
                    audioCache.remove(keys[0]);
                    keys = null;
                }
            } else {
                currentClip = audioCache.get(filepath);
                currentClip.setFramePosition(0);
                currentClip.stop();
            }

            currentClip.addLineListener(this);
            setVolume(volume);
            setMute(mute);

            if (loop) {
                currentClip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                currentClip.start();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Issues when playing WAV sound", e);
        }

    }

    /**
     * Play compressed sound (mp3 or ogg files) The sounds are not cached in
     * this case.
     * 
     * @param filepath filepath the file path to the sound
     * @param loop Should the sound clip be looped?
     */
    public void startPlayCompressedSound(String filepath, boolean loop) {
        stop();

        AudioInputStream din = null;
        looping = loop;

        do {
            try {
                File file = new File(filepath);
                AudioInputStream in = AudioSystem.getAudioInputStream(file);
                AudioFormat baseFormat = in.getFormat();
                AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16,
                        baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);

                din = AudioSystem.getAudioInputStream(decodedFormat, in);

                DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);

                currentLine = (SourceDataLine) AudioSystem.getLine(info);

                if (currentLine != null) {
                    currentLine.addLineListener(this);
                    currentLine.open(decodedFormat);
                    setVolume(volume);
                    setMute(mute);

                    byte[] data = new byte[decodedFormat.getSampleSizeInBits()];
                    // Start
                    currentLine.start();

                    int nBytesRead = 0;

                    while ((nBytesRead = din.read(data, 0, data.length)) != -1) {
                        currentLine.write(data, 0, nBytesRead);
                    }

                    currentLine.drain();
                    currentLine.stop();

                }

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Issues when playing compressed sound", e);
            } finally {
                if (din != null) {
                    try {
                        din.close();
                        din = null;
                    } catch (IOException e) {
                    }
                }

            }
        } while (looping);

    }

    /**
     * Play compressed sound (mp3 or ogg files) The sounds are not cached in
     * this case.
     * 
     * @param filepath filepath the file path to the sound
     * @param loop Should the sound clip be looped?
     */
    public void startMidiSound(String filepath, boolean loop) {

        looping = loop;

        try {
            // --This tells you what your MidiDevices are
            sequencer = MidiSystem.getSequencer();
            synthesizer = MidiSystem.getSynthesizer();
            sequencer.open();
            synthesizer.open();

            synthReceiver = synthesizer.getReceiver();
            seqTransmitter = sequencer.getTransmitter();
            seqTransmitter.setReceiver(synthReceiver);

            // From file
            Sequence sequence = MidiSystem.getSequence(new File(filepath));

            sequencer.setSequence(sequence);
            sequencer.addMetaEventListener(this);
            setVolume(volume);
            setMute(mute);
            if (looping) {
                sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
                sequencer.start();
            } else {
                sequencer.start();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Issues when playing compressed sound", e);
        }

    }

    /**
     * Play a clip once.
     * 
     * @param filepath the filepath to the sound file.
     */
    public void play(String filepath) {
        this.startPlay(filepath, false);
    }

    /**
     * Play the clip in a loop.
     * 
     * @param filepath the filepath to the sound file.
     */
    public void loop(String filepath) {
        this.startPlay(filepath, true);
    }

    /**
     * Stops the playing clip.
     */
    public void stop() {

        looping = false;

        if (currentClip != null) {
            currentClip.stop();
            currentClip.removeLineListener(this);
            currentClip = null;
        }

        if (currentLine != null) {
            currentLine.drain();
            currentLine.close();
            currentLine.removeLineListener(this);
            currentLine = null;
        }

        if (sequencer != null) {
            sequencer.stop();
            sequencer.close();
            sequencer.removeMetaEventListener(this);
            sequencer = null;
        }

    }

    /**
     * Gets the volume of the audio player.
     * 
     * @return volume (0.0 to 1.0)
     */
    public float getVolume() {
        return volume;
    }

    /**
     * Sets the volume for the audio player.
     * 
     * @param volume (0.0 quiet, .5 medium, 1.0 loud) (0.0 to 1.0 valid range)
     */
    public void setVolume(float volume) {
        if ((volume < 0F) && (volume > 1F))
            throw new IllegalArgumentException("Volume invalid: " + volume);

        this.volume = volume;

        // Set volume
        if (currentClip != null) {
            // Note: No linear volume control for the clip,
            // so use gain control.
            // Linear volume = pow(10.0, gainDB/20.0)
            // Note Math.log10 is Java 1.5 or better.
            // float gainLog10 = (float) Math.log10(volume);
            float gainLog10 = (float) (Math.log(volume) / Math.log(10F));
            float gain = gainLog10 * 20F;
            FloatControl gainControl = (FloatControl) currentClip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(gain);
        }

        if (currentLine != null) {
            // Note: No linear volume control for the clip,
            // so use gain control.
            // Linear volume = pow(10.0, gainDB/20.0)
            // Note Math.log10 is Java 1.5 or better.
            // float gainLog10 = (float) Math.log10(volume);
            float gainLog10 = (float) (Math.log(volume) / Math.log(10F));
            float gain = gainLog10 * 20F;
            FloatControl gainControl = (FloatControl) currentLine.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(gain);
        }

        if (sequencer != null) {
            setVolumeSequencer(volume);
        }
    }

    /**
     * Checks if the audio player is muted.
     * 
     * @return true if muted.
     */
    public boolean isMute() {
        return mute;
    }

    /**
     * Sets if the audio player is mute or not.
     * 
     * @param mute is audio player mute?
     */
    public void setMute(boolean mute) {
        // Set mute value.
        this.mute = mute;

        if (currentClip != null) {
            BooleanControl muteControl = (BooleanControl) currentClip.getControl(BooleanControl.Type.MUTE);
            muteControl.setValue(mute);
        }

        if (currentLine != null) {
            BooleanControl muteControl = (BooleanControl) currentLine.getControl(BooleanControl.Type.MUTE);
            muteControl.setValue(mute);
        }

        if (sequencer != null) {
            muteSequencer(mute);
        }
    }

    private void muteSequencer(boolean mute) {
        Sequence sequence = sequencer.getSequence();
        int tracks = sequence.getTracks().length;

        for (int i = 0; i < tracks; i++) {
            sequencer.setTrackMute(i, mute);
        }
    }

    private void setVolumeSequencer(float volume) {
        // convert to a range 0 to 127
        int convert = (int) (volume * 127);
        MidiChannel[] channels = synthesizer.getChannels();
        for (int i = 0; i < channels.length; i++) {
            channels[i].controlChange(7, convert);
        }

    }

    /**
     * LineListener interface. This method is called when an event occurs during
     * the sound playing: end of sound...
     */
    public void update(LineEvent event) {
        if (event.getType() == LineEvent.Type.STOP || event.getType() == LineEvent.Type.CLOSE) {

            if (event.getSource().equals(currentClip)) {
                currentClip.stop();
            } else {
                Line line = event.getLine();
                line.close();
                line.removeLineListener(this);
            }
        }
    }

    public void cleanAudioPlayer() {
        stop();
        audioCache.clear();
        sound_player = null;
    }

    /*
     * For handling midi player events
     */
    public void meta(MetaMessage meta) {
        if (meta.getType() == 47) {
            if (sequencer != null) {
                sequencer.stop();
                sequencer.close();
                sequencer.removeMetaEventListener(this);
                sequencer = null;
            }
        }

    }
}