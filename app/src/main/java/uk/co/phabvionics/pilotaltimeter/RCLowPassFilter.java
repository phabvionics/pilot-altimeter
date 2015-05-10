package uk.co.phabvionics.pilotaltimeter;

public class RCLowPassFilter {
    private float mSample; // input sample
    private float mTime; // time in arbitrary units
    private float mFunction; // value of RC-filtered f(mTime)
    private float mTimeConstant; // value of 1/RC
    private boolean mInitialised; // false if no samples yet set

    public RCLowPassFilter(float timeConstant) {
        mTimeConstant = timeConstant;
    }

    public float SampleAtTime(float time) {
        float deltaTime = time - mTime;
        float deltaSample = mSample - mFunction;
        return mFunction + deltaSample * (1.0f - (float)Math.exp(-deltaTime / mTimeConstant));
    }

    public float NewSample(float sample, float time) {
        if (!mInitialised) {
            mFunction = sample;
            mInitialised = true;
        } else {
            mFunction = SampleAtTime(time);
        }
        mSample = sample;
        mTime = time;
        return mFunction;
    }
}
