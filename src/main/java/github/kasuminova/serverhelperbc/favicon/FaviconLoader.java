package github.kasuminova.serverhelperbc.favicon;

import net.md_5.bungee.api.Favicon;

import javax.imageio.ImageIO;
import java.io.File;

public class FaviconLoader {

    protected final File faviconFile;
    protected Favicon favicon = null;

    public FaviconLoader(final File dataFolder) {
        this.faviconFile = new File(dataFolder.getPath() + File.separator + "favicon.png");
    }

    public void load() throws Exception {
        favicon = Favicon.create(ImageIO.read(faviconFile));
    }

    public File getFaviconFile() {
        return faviconFile;
    }

    public Favicon getFavicon() {
        return favicon;
    }

}
