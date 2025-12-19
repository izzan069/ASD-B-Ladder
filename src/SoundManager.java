import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.net.URL;

public class SoundManager {

    private Clip bgmClip;
    private FloatControl volumeControl;

    // Play Sound Effect (Sekali main)
    public void play(String filename) {
        new Thread(() -> {
            try {
                // Pastikan file audio ada di folder src/resources/
                URL url = getClass().getResource("/resources/" + filename);
                if (url == null) {
                    // System.err.println("Audio missing: " + filename); // Uncomment untuk debug
                    return;
                }
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);
                setClipVolume(clip, -5.0f);
                clip.start();
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }).start();
    }

    // Play Background Music (Looping)
    public void playBackgroundMusic(String filename) {
        new Thread(() -> {
            try {
                URL url = getClass().getResource("/resources/" + filename);
                if (url == null) return;

                AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
                bgmClip = AudioSystem.getClip();
                bgmClip.open(audioIn);

                if (bgmClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    volumeControl = (FloatControl) bgmClip.getControl(FloatControl.Type.MASTER_GAIN);
                }
                setVolume(0.5f); // Default volume 50%
                bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                bgmClip.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void setVolume(float value) {
        if (volumeControl == null) return;
        float min = -80.0f;
        float max = 6.0f;
        if (value <= 0.01f) {
            volumeControl.setValue(min);
        } else {
            float dB = (float) (Math.log10(value) * 20.0);
            if (dB > max) dB = max;
            if (dB < min) dB = min;
            volumeControl.setValue(dB);
        }
    }

    private void setClipVolume(Clip clip, float dB) {
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(dB);
            }
        } catch (Exception e) {}
    }
}