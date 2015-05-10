package uk.co.phabvionics.pilotaltimeter;

public class IIRLowPassFilter {
    private float mAlpha;
    private float mSampleM1;
    private boolean mAlreadyRun;

    //public IIRLowPassFilter(float alpha) {
    //	mAlpha = alpha;
    //}

    public IIRLowPassFilter(int samplesPerTimeConstant) {
        mAlpha = (float) Math.exp(-1.0/samplesPerTimeConstant);
    }

    public IIRLowPassFilter(float samplesPerTimeConstant) {
        mAlpha = (float) Math.exp(-1.0/samplesPerTimeConstant);
    }

    public float Filter(float sample) {
        float newSample;

        if (!mAlreadyRun) {
            mSampleM1 = sample;
            mAlreadyRun = true;
        }

        newSample = sample * (1 - mAlpha) + mSampleM1 * mAlpha;
        mSampleM1 = newSample;

        return newSample;
    }
}
