import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;

import processing.core.PApplet;

/** Class to generate start tone for experiments, if specified. Note this tone
 * implementation block on the main thread and needs to be updated
 */
public class SoundUtils extends PApplet {

  public float SAMPLE_RATE = 8000f;

  /**
   * play a tone at the maximum volumn
   *
   * @param hz    frequence of the tone
   * @param msecs duration of the tone
   */
  public void tone(int hz, int msecs) 
     throws LineUnavailableException 
  {
     tone(hz, msecs, 1.0);
  }

  /**
   * play a tone at the specified volumn
   *
   * @param hz    frequence of the tone
   * @param msecs duration of the tone
   * @param vol   folumn for the tone
   */
  public void tone(int hz, int msecs, double vol)
      throws LineUnavailableException 
  {
    byte[] buf = new byte[1];
    AudioFormat af = 
        new AudioFormat(
            SAMPLE_RATE, // sampleRate
            8,           // sampleSizeInBits
            1,           // channels
            true,        // signed
            false);      // bigEndian
    SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
    sdl.open(af);
    sdl.start();
    for (int i=0; i < msecs*8; i++) {
      double angle = i / (SAMPLE_RATE / hz) * 2.0 * Math.PI;
      buf[0] = (byte)(Math.sin(angle) * 127.0 * vol);
      sdl.write(buf,0,1);
    }
    sdl.drain();
    sdl.stop();
    sdl.close();
  }
}
