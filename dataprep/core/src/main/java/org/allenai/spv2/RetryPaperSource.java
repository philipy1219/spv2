package org.allenai.spv2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * An instance of PaperSource that wraps another, and retries a bunch of times to get the paper
 */
public class RetryPaperSource implements PaperSource {
    private static Logger logger = LoggerFactory.getLogger(RetryPaperSource.class);

    private final PaperSource inner;
    private final int tries;

    public RetryPaperSource(final PaperSource inner) {
        this(inner, 3);
    }

    public RetryPaperSource(final PaperSource inner, final int tries) {
        this.inner = inner;
        this.tries = tries;
    }

    private void wait(int seconds) {
        final long endTime = System.currentTimeMillis() + 1000 * seconds;
        while(System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(Math.max(endTime - System.currentTimeMillis() + 1, 1));
            } catch(final InterruptedException e) {
                // do nothing
            }
        }
    }

    @Override
    public InputStream getPdf(final String paperId) throws IOException {
        int triesLeft = tries;
        int previousWait = 1;
        int nextWait = 1;

        while(true) {
            triesLeft -= 1;
            try {
                return inner.getPdf(paperId);
            } catch(final IOException e) {
                logger.warn(
                    "{} while downloading paper {}, {} tries left",
                    e.getClass().getSimpleName(),
                    paperId,
                    triesLeft);
                if(triesLeft <= 0) {
                    throw e;
                } else {
                    wait(nextWait);
                    final int waited = nextWait;
                    nextWait += previousWait;
                    previousWait = waited;
                }
            }
        }
    }
}
