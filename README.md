# Project Pickup

A high-performance, real-time audio processing application designed for system-wide audio manipulation. Project Pickup intercepts system audio streams and applies a customizable chain of DSP filters before outputting the processed audio. Built with a hybrid architecture combining Java's flexibility with native C/C++ performance for demanding audio processing operations.

## 🎯 Features

### Core Capabilities
- **Real-Time Audio Processing**: Ultra-low latency processing optimized for live audio
- **System Audio Interception**: Captures and processes all system audio output
- **Modular Filter Architecture**: Chain multiple filters with configurable ordering
- **Hybrid Performance**: Java application logic with native DSP implementations
- **Cross-Platform Support**: Windows, macOS, and Linux compatibility
- **Configuration Persistence**: Save and load custom filter presets

### Supported Filter Types
- **Standard IIR Filters**: Butterworth, Bessel, Chebyshev I & II with configurable parameters
- **10-Band Graphic Equalizer**: Professional-grade frequency shaping with adjustable Q factor
- **Dynamic Range Processing**: Multi-parameter limiter with lookahead capability
- **Stereo Processing**: Channel balance adjustment and spatial manipulation
- **Extensible Framework**: Easy integration of custom filters

## 🏗️ Architecture Deep Dive

### Data Flow Pipeline

The application implements a sophisticated real-time audio pipeline:

```
┌─────────────┐    ┌──────────────┐    ┌───────────────────┐    ┌──────────────┐    ┌──────────────┐
│ System Audio│───▶│ Audio Capture│───▶│   Normalization   │───▶│ Filter Chain │───▶│ Audio Output │
│   Source    │    │ (TargetLine) │    │(byte[] > double[])│    │ (Sequential) │    │ (SourceLine) │
└─────────────┘    └──────────────┘    └───────────────────┘    └──────────────┘    └──────────────┘
                                                ▲                       │
                                                │                       ▼
                                        ┌───────────────────┐    ┌───────────────────────┐
                                        │  Denormalization  │◀───│      Filter Rack      │
                                        │ (double[]→byte[]) │    │  (ArrayList<Object>)  │
                                        └───────────────────┘    └───────────────────────┘
```

### Thread Architecture

```
Main Application Thread
├── AudioPipeline (ExecutorService)
│   ├── Audio I/O Management
│   ├── Buffer Conversion
│   └── Processing Loop (15ms buffer cycles)
├── Filter Management
│   ├── Filter Chain Orchestration
│   ├── Dynamic Filter Addition/Removal
│   └── Sequential Processing
└── Configuration System
    ├── JSON Serialization
    └── Preset Management
```

### Memory Management

- **Buffer Size**: Dynamically calculated as `sampleRate * channels * (bitDepth/8) * 0.015` (15ms latency)
- **Processing Strategy**: In-place modification where possible, copy-on-write for safety
- **Native Integration**: JNI with efficient array passing and minimal GC pressure

## 🚀 Quick Start

### Prerequisites

```bash
# Java Development Kit
java -version  # Requires Java 8+

# Audio System Requirements
# - Working audio input/output devices
# - Sufficient buffer sizes for real-time processing
# - Platform-specific native libraries (included)
```

### Basic Implementation

```java
import AudioPipeline.AudioPipeline;
import AudioProcessingRangler.AudioProcessingRangler;
import StandardFilter.StandardFilter;
import NativeFilter.GraphicEqualizer;

public class BasicAudioProcessor {
    public static void main(String[] args) {
        // Initialize the audio pipeline
        AudioPipeline pipeline = new AudioPipeline();
        AudioProcessingRangler rangler = new AudioProcessingRangler();
        
        // Get audio format information
        AudioFormat format = pipeline.getFormat();
        System.out.printf("Processing: %.1f Hz, %d-bit, %d channels%n", 
                         format.getSampleRate(), 
                         format.getSampleSizeInBits(), 
                         format.getChannels());
        
        // Create and configure filters
        try {
            // Add a low-pass filter to remove high frequencies
            StandardFilter lowpass = new StandardFilter(
                StandardFilter.FilterType.Butterworth, 
                6,  // 6th order for sharp rolloff
                format.getSampleRate(), 
                Optional.empty()
            );
            lowpass.setLowpass(4000.0);  // 4kHz cutoff
            rangler.addFilter(lowpass, 0);
            
            // Add graphic equalizer for fine-tuning
            int bufferSize = (int)(format.getSampleRate() * 
                                  format.getChannels() * 
                                  (format.getSampleSizeInBits() / 8) * 0.015);
            
            GraphicEqualizer eq = new GraphicEqualizer(
                format.getChannels(),
                bufferSize,
                format.getSampleRate()
            );
            
            // Boost bass, cut harsh mids
            double[] gains = {3.0, 2.0, 0.0, -2.0, -1.0, 0.0, 1.0, 0.0, -1.0, 0.0};
            eq.setGains(gains);
            rangler.addFilter(eq, 1);
            
            // Start processing
            pipeline.setEqualizer(rangler);
            pipeline.start();
            
            System.out.println("Audio processing started. Press Enter to stop...");
            System.in.read();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            pipeline.stop();
        }
    }
}
```

### Advanced Filter Chaining

```java
// Create a professional mastering chain
public void createMasteringChain(AudioProcessingRangler rangler, AudioFormat format) {
    int bufferSize = calculateBufferSize(format);
    
    // 1. Input conditioning with high-pass filter
    StandardFilter highpass = new StandardFilter(
        StandardFilter.FilterType.Butterworth, 4, format.getSampleRate(), Optional.empty()
    );
    highpass.setHighpass(20.0);  // Remove subsonic content
    rangler.addFilter(highpass, 0);
    
    // 2. Graphic EQ for tonal shaping
    GraphicEqualizer eq = new GraphicEqualizer(format.getChannels(), bufferSize, format.getSampleRate());
    double[] masteringCurve = {0.5, 0.0, -0.5, 0.0, 1.0, 0.0, 1.5, 0.0, -1.0, -2.0};
    eq.setGains(masteringCurve);
    eq.setQ(3.0);  // Tighter Q for precision
    rangler.addFilter(eq, 1);
    
    // 3. Stereo enhancement
    ChannelBalancer balancer = new ChannelBalancer(
        format.getChannels(), bufferSize, format.getSampleRate(), 0.2
    );
    rangler.addFilter(balancer, 2);
    
    // 4. Final limiting for loudness
    Limiter limiter = new Limiter(
        format.getChannels(), bufferSize, format.getSampleRate(),
        -0.1,    // threshold (just below 0dB)
        0.05,    // fast attack
        50.0,    // moderate release
        5.0      // lookahead for transparent limiting
    );
    rangler.addFilter(limiter, 3);
}
```

## 📚 Comprehensive API Reference

### AudioPipeline Class

The core audio processing engine that manages system audio I/O and coordinates the filter chain.

#### Constructor & Initialization

```java
public AudioPipeline()
```
**Behavior**: 
- Queries system for default audio format
- Initializes internal audio format parameters
- Prepares atomic threading controls
- **Throws**: `RuntimeException` if system audio is unavailable

**Internal Format Detection**:
```java
// Automatically detected properties
protected float sampleRate;      // Usually 44100.0 or 48000.0 Hz
protected int bitDepth;          // 8, 16, 24, or 32 bits
protected int channels;          // 1 (mono) or 2 (stereo)
protected boolean bigEndian;     // Platform-dependent byte order
protected AudioFormat.Encoding encoding; // PCM_SIGNED or PCM_FLOAT
```

#### Core Methods

```java
public void setEqualizer(AudioProcessingRangler equalizer)
```
**Purpose**: Associates a filter chain with the pipeline
**Thread Safety**: Can be called while pipeline is running
**Parameters**: 
- `equalizer`: The processing chain (null clears current chain)

```java
public AudioFormat getFormat()
```
**Returns**: Complete audio format specification
**Usage**: Essential for configuring filters with correct parameters

```java
public void start()
```
**Behavior**:
- Opens `TargetDataLine` and `SourceDataLine` with system format
- Initializes `ExecutorService` with single processing thread
- Begins continuous audio processing loop
- **Thread Safe**: Ignores duplicate start calls
- **Throws**: `RuntimeException` for audio line failures

```java
public void stop()
```
**Behavior**:
- Gracefully stops processing thread with 500ms timeout
- Properly drains and closes audio lines
- Cleans up all resources
- **Thread Safe**: Uses atomic boolean for clean shutdown

#### Internal Processing Details

**Buffer Management**:
```java
// Dynamic buffer sizing for 15ms latency
int bufferSize = (int)(sampleRate * channels * (bitDepth / 8) * 0.015);
```

**Audio Conversion Pipeline**:
```java
private double[] toDoubleArray(byte[] byteArray, int bytesRead)
private byte[] toByteArray(double[] doubleArray, int byteLength)
```

**Supported Formats**:
- **8-bit**: Unsigned PCM (0-255) → normalized [-1.0, 1.0]
- **16-bit**: Signed PCM (-32768 to 32767) → normalized [-1.0, 1.0]
- **32-bit**: Signed PCM or IEEE float
- **64-bit**: IEEE double precision float

**Normalization Constants**:
```java
private static final double NORM_8_BIT = 127.0;
private static final double NORM_16_BIT = 32767.0;
private static final double NORM_32_BIT_INT = 2147483647.0;
```

### AudioProcessingRangler Class

Manages the sequential chain of audio filters with dynamic manipulation capabilities.

#### Filter Management

```java
public void addFilter(Object filter, int rackPosition)
```
**Behavior**: Inserts filter at specified position, shifting others as needed
**Supported Types**: `StandardFilter`, `NativeFilterInterface` implementations
**Dynamic**: Can modify chain during processing

```java
public boolean removeFilter(int filterPosition) throws EmptyFilterRackException, IndexOutOfBoundsException
```
**Returns**: `true` on successful removal
**Error Handling**: Comprehensive bounds checking with specific exceptions

```java
public Object getFilter(int filterPosition) throws EmptyFilterRackException, IndexOutOfBoundsException
```
**Purpose**: Non-destructive filter retrieval for inspection or reconfiguration

#### Processing Engine

```java
public double[] processData(double[] buffer)
```
**Algorithm**:
```java
for (Object filter : filterRack) {
    if (filter instanceof StandardFilter) {
        // Process sample-by-sample through IIR cascade
        Cascade settings = ((StandardFilter)filter).getSettings();
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = settings.filter(buffer[i]);
        }
    } else if (filter instanceof NativeFilterInterface) {
        // Batch processing through native implementation
        buffer = ((NativeFilterInterface)filter).process(buffer);
    }
}
```

**Performance Notes**:
- StandardFilters: Sample-by-sample IIR processing (high precision)
- Native Filters: Block-based processing (high performance)
- In-place modification where possible

#### Utility Methods

```java
public boolean isEmpty()     // Check if filter rack is empty
public int size()           // Get current filter count
public boolean isFull()     // Always returns false (unlimited capacity)
```

### StandardFilter Class

Java-based IIR filter implementation using the iirj library for precise digital signal processing.

#### Filter Types & Characteristics

```java
public enum FilterType { 
    Butterworth,    // Maximally flat passband
    Bessel,         // Linear phase response
    ChebyshevI,     // Equiripple passband
    ChebyshevII     // Equiripple stopband
}
```

#### Constructor

```java
public StandardFilter(FilterType filterType, int order, double sampleRate, Optional<Double> rippleDb)
```

**Parameters**:
- `filterType`: Determines frequency response characteristics
- `order`: Filter order (higher = steeper rolloff, more CPU)
- `sampleRate`: Must match AudioPipeline sample rate
- `rippleDb`: Required for Chebyshev filters (passband/stopband ripple)

**Typical Orders**:
- **2nd order**: Gentle rolloff, minimal phase distortion
- **4th order**: Good balance of selectivity and efficiency
- **6th+ order**: Sharp cutoffs, potential stability issues

#### Filter Configuration Methods

```java
public void setLowpass(double cutoffFrequency)
```
**Use Case**: Remove high-frequency noise, anti-aliasing
**Typical Values**: 4000-8000 Hz for voice processing

```java
public void setHighpass(double cutoffFrequency)
```
**Use Case**: Remove DC offset, rumble, low-frequency noise
**Typical Values**: 20-100 Hz for most applications

```java
public void setBandpass(double centerFrequency, double frequencyWidth)
```
**Use Case**: Isolate specific frequency ranges
**Parameters**: 
- `centerFrequency`: Peak response frequency
- `frequencyWidth`: -3dB bandwidth

```java
public void setBandstop(double centerFrequency, double frequencyWidth)
```
**Use Case**: Notch filtering for specific interference (50/60 Hz hum)

### Native Filter Implementations

High-performance DSP implementations using JNI for computationally intensive operations.

#### GraphicEqualizer

Professional 10-band graphic equalizer with ISO standard center frequencies.

```java
public GraphicEqualizer(int channels, int bufferSize, float sampleRate, double[] bandGains)
```

**Frequency Bands**:
```
31 Hz, 63 Hz, 125 Hz, 250 Hz, 500 Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
```

**Parameters**:
```java
public void setGains(double[] bandGains)  // Range: [-2.0, 2.0] (linear gain)
public void setQ(double qFactor)          // Range: [0.1, 20.0], default: 6.0
```

**Gain Interpretation**:
- `0.0`: No change (0 dB)
- `1.0`: +6 dB boost
- `-1.0`: -6 dB cut
- `2.0`: +12 dB maximum boost

**Usage Examples**:
```java
// "V-shaped" sound (enhanced bass and treble)
double[] vShape = {1.5, 1.0, 0.0, -0.5, -1.0, -1.0, -0.5, 0.0, 1.0, 1.5};
eq.setGains(vShape);

// Vocal clarity enhancement
double[] vocal = {-0.5, 0.0, 0.0, 0.5, 1.0, 1.5, 1.0, 0.0, -0.5, -1.0};
eq.setGains(vocal);
```

#### Limiter

Advanced lookahead limiter for dynamic range control and loudness maximization.

```java
public Limiter(int channels, int bufferSize, double sampleRate, 
               double threshold_dB, double attack_ms, double release_ms, double lookahead_ms)
```

**Parameters & Typical Values**:
```java
setThreshold(-0.1)      // Just below 0 dBFS to prevent clipping
setAttackTime(0.05)     // 0.05-1.0 ms for transparent limiting
setReleaseTime(50.0)    // 10-100 ms for natural dynamics
setLookahead(5.0)       // 1-10 ms for artifact-free processing
```

**Algorithm Features**:
- **Lookahead**: Prevents overshoot by analyzing future samples
- **Smooth Gain Reduction**: Exponential attack/release curves
- **Frequency-Independent**: Maintains spectral balance
- **Low Distortion**: Advanced anti-aliasing and oversampling

#### ChannelBalancer

Stereo field manipulation and channel balance adjustment.

```java
public ChannelBalancer(int channels, int bufferSize, double sampleRate, double preference)
```

**Preference Parameter**:
- `0.0`: Perfect center balance
- `-1.0`: Full left channel preference
- `+1.0`: Full right channel preference
- `±0.2`: Subtle stereo widening/narrowing

**Applications**:
```java
// Subtle stereo widening
ChannelBalancer widener = new ChannelBalancer(2, bufferSize, sampleRate, 0.15);

// Mono compatibility check
ChannelBalancer mono = new ChannelBalancer(2, bufferSize, sampleRate, 0.0);
```

### ConfigParser Class

Comprehensive configuration management using Jackson JSON serialization.

#### Configuration Storage

```java
public boolean addConfig(ArrayList<Object> filterValues, Optional<String> configName)
```
**Storage Location**: `src/main/resources/native/configs/`
**File Format**: JSON with complete filter state serialization
**Naming**: Automatic timestamp naming if `configName` is empty

**Example Configuration Structure**:
```json
{
  "filters": [
    {
      "type": "StandardFilter",
      "filterType": "Butterworth",
      "order": 4,
      "sampleRate": 44100.0,
      "configuration": "lowpass",
      "cutoffFrequency": 4000.0
    },
    {
      "type": "GraphicEqualizer",
      "channels": 2,
      "sampleRate": 44100.0,
      "bandGains": [0.0, 0.5, 1.0, 0.0, -0.5, 0.0, 1.0, 0.5, 0.0, -1.0],
      "qFactor": 6.0
    }
  ],
  "metadata": {
    "created": "2023-10-15T14:30:00Z",
    "description": "Vocal Enhancement Preset"
  }
}
```

#### Configuration Retrieval

```java
public ArrayList<Object> getConfig(String configName) throws ConfigIOAccessException
```
**Behavior**: 
- Deserializes JSON to reconstruct exact filter states
- Validates filter parameters during loading
- Maintains object type information and relationships

## 📊 Performance Benchmarks

### Typical Performance Characteristics

| Filter Type | CPU Usage (44.1kHz Stereo) | Latency Impact | Memory Usage |
|-------------|---------------------------|----------------|--------------|
| StandardFilter (2nd order) | 0.5-1% | Minimal | 2KB |
| StandardFilter (8th order) | 2-4% | Low | 8KB |
| GraphicEqualizer | 3-7% | Low | 16KB |
| Limiter | 2-5% | 5ms lookahead | 32KB |
| ChannelBalancer | 0.1-0.5% | Minimal | 1KB |
| Full Chain (5 filters) | 8-15% | 15-25ms total | 64KB |

### Optimization Guidelines

- **Single-threaded Processing**: All filters run in audio thread - avoid blocking operations
- **Filter Order**: Place lightweight filters first, heavy processing last  
- **Native vs Java**: Use native filters for CPU-intensive operations (>5% CPU)
- **Buffer Management**: Larger buffers reduce CPU overhead but increase latency
- **Sample Rate**: Higher sample rates exponentially increase CPU requirements

## 🤝 Contributing

### Development Environment Setup

```bash
# Clone repository
git clone https://github.com/yourusername/project-pickup.git
cd project-pickup

# Build native libraries (platform-specific)
# Windows (MinGW)
gcc -shared -fPIC -o graphic_equalizer.dll -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/win32" src/native/graphic_equalizer.c

# Linux
gcc -shared -fPIC -o libgraphic_equalizer.so -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" src/native/graphic_equalizer.c

# macOS  
gcc -shared -fPIC -o libgraphic_equalizer.dylib -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin" src/native/graphic_equalizer.c

# Compile Java sources
javac -cp "lib/*" -d build src/main/java/**/*.java

# Run with native library path
java -Djava.library.path=build/native -cp "build:lib/*" YourMainClass
```

## 📄 License

- GNU GENERAL PUBLIC LICENSE V3

**Built with ❤️ for the audio processing community**
