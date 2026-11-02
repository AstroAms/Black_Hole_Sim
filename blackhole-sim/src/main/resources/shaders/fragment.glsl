#version 330 core
out vec4 FragColor;

in vec2 vUV;

uniform vec2 uResolution;
uniform float uTime;
uniform vec3 uCamPos;
uniform vec3 uCamForward;
uniform vec3 uCamUp;
uniform vec3 uCamRight;
uniform float uFov;
uniform float uBhRadius;
uniform int uEnableLensing;
uniform int uEnableDisk;
uniform int uEnableDoppler;
uniform int uEnableGrid;
uniform int uEnableStars;
uniform float uDiskBrightness;
uniform sampler2D uStarsTexture;

const float PI = 3.14159265358979323846;

// Fast noise and hash functions for plasma filaments and stars
float hash1(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float hash3(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise2D(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash1(i);
    float b = hash1(i + vec2(1.0, 0.0));
    float c = hash1(i + vec2(0.0, 1.0));
    float d = hash1(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// Multi-octave turbulence for accretion disk plasma
float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 rot = mat2(0.8, -0.6, 0.6, 0.8);
    for (int i = 0; i < 4; ++i) {
        v += a * noise2D(p);
        p = rot * p * 2.05;
        a *= 0.5;
    }
    return v;
}

// Realistic Blackbody/Plasma Color Ramp
vec3 diskColorRamp(float t) {
    vec3 c0 = vec3(0.08, 0.01, 0.002); // Deep crimson edge
    vec3 c1 = vec3(0.75, 0.12, 0.01);  // Burning ruby red
    vec3 c2 = vec3(1.0, 0.42, 0.05);   // Blazing fire orange
    vec3 c3 = vec3(1.0, 0.85, 0.22);   // Brilliant golden amber
    vec3 c4 = vec3(1.0, 0.98, 0.88);   // Incandescent core white
    vec3 c5 = vec3(0.75, 0.90, 1.0);   // Extreme Doppler blue-white
    
    if (t < 0.20) return mix(c0, c1, t / 0.20);
    else if (t < 0.45) return mix(c1, c2, (t - 0.20) / 0.25);
    else if (t < 0.72) return mix(c2, c3, (t - 0.45) / 0.27);
    else if (t < 0.90) return mix(c3, c4, (t - 0.72) / 0.18);
    else return mix(c4, c5, (t - 0.90) / 0.10);
}

// Null geodesic acceleration in Schwarzschild spacetime:
// a = -1.5 * rs * |L|^2 / r^5 * x
vec3 getGeodesicAcc(vec3 pos, vec3 dir, float rs, float L2) {
    float r2 = dot(pos, pos);
    float r = sqrt(r2);
    float r5 = r2 * r2 * r;
    return -1.5 * rs * L2 * pos / max(r5, 1e-4);
}

void main() {
    vec2 fragCoord = gl_FragCoord.xy;
    vec2 ndc = (fragCoord - 0.5 * uResolution) / uResolution.y;
    
    float tanHalfFov = tan(uFov * 0.5);
    vec3 rayDir = normalize(ndc.x * tanHalfFov * uCamRight + ndc.y * tanHalfFov * uCamUp + uCamForward);
    vec3 rayPos = uCamPos;
    
    float rs = max(0.1, uBhRadius);
    float r_in = 2.4 * rs;      // Innermost stable circular orbit boundary
    float r_out = 7.5 * rs;     // Outer edge of accretion disk
    float r_escape = 40.0 * rs; // Outer escape boundary
    
    vec3 Lvec = cross(rayPos, rayDir);
    float L2 = dot(Lvec, Lvec);
    
    vec3 accumColor = vec3(0.0);
    float accumAlpha = 0.0;
    
    bool hitHorizon = false;
    vec3 prevPos = rayPos;
    
    const int MAX_STEPS = 160;
    
    for (int i = 0; i < MAX_STEPS; ++i) {
        float r = length(rayPos);
        
        // Check event horizon capture
        if (r <= rs * 1.01) {
            hitHorizon = true;
            break;
        }
        
        // Check escape
        if (r > r_escape) {
            break;
        }
        
        // Adaptive step size: smaller near horizon, larger far away
        float dt = clamp(0.075 * r, 0.025 * rs, 0.6 * rs);
        
        // Velocity Verlet geodesic integration step
        vec3 a1 = (uEnableLensing == 1) ? getGeodesicAcc(rayPos, rayDir, rs, L2) : vec3(0.0);
        vec3 nextPos = rayPos + rayDir * dt + 0.5 * a1 * dt * dt;
        vec3 midDir = rayDir + a1 * dt;
        vec3 a2 = (uEnableLensing == 1) ? getGeodesicAcc(nextPos, midDir, rs, L2) : vec3(0.0);
        vec3 nextDir = normalize(rayDir + 0.5 * (a1 + a2) * dt);
        
        // Accretion disk intersection (Equatorial plane y = 0)
        if (uEnableDisk == 1 && prevPos.y * nextPos.y <= 0.0) {
            float denom = abs(prevPos.y) + abs(nextPos.y) + 1e-6;
            float tPlane = abs(prevPos.y) / denom;
            vec3 hitPos = mix(prevPos, nextPos, tPlane);
            float rHit = length(hitPos.xz);
            
            if (rHit >= r_in && rHit <= r_out) {
                float normR = (rHit - r_in) / (r_out - r_in);
                
                // Shakura-Sunyaev thermal profile
                float tempRel = pow(r_in / rHit, 0.75) * pow(max(0.001, 1.0 - sqrt(r_in / rHit)), 0.25);
                float tempNorm = clamp(tempRel * 3.3, 0.0, 1.0);
                
                // Dynamic differential Keplerian rotation (Omega ~ r^-1.5)
                float phi = atan(hitPos.z, hitPos.x);
                float omega = 2.4 * pow(r_in / rHit, 1.5);
                float rotPhi = phi - uTime * omega;
                
                // Procedural turbulent filaments & spiral density waves
                vec2 polarCoord = vec2(rotPhi * 2.5, normR * 6.0);
                float turb = fbm(polarCoord);
                float spiralArm = sin(rotPhi * 3.0 + normR * 12.0) * 0.5 + 0.5;
                float plasmaDensity = (0.55 + 0.35 * turb + 0.15 * spiralArm);
                plasmaDensity *= smoothstep(0.0, 0.07, normR) * smoothstep(1.0, 0.88, normR);
                
                // Relativistic Doppler Beaming & Gravitational Redshift
                float shift = 1.0;
                float dopplerBoost = 1.0;
                
                if (uEnableDoppler == 1) {
                    float beta = sqrt(rs / (2.0 * rHit)); // v / c
                    vec3 vDir = normalize(vec3(-hitPos.z, 0.0, hitPos.x)); // Counter-clockwise orbital velocity
                    vec3 toObserver = -rayDir;
                    float cosAngle = dot(vDir, toObserver);
                    float gamma = 1.0 / sqrt(max(0.01, 1.0 - beta * beta));
                    float dopplerFactor = 1.0 / (gamma * (1.0 - beta * cosAngle));
                    float gravRedshift = sqrt(max(0.01, 1.0 - rs / rHit));
                    shift = dopplerFactor * gravRedshift;
                    dopplerBoost = pow(clamp(shift, 0.15, 3.8), 3.2);
                }
                
                float effTemp = clamp(tempNorm * pow(shift, 0.9), 0.0, 1.0);
                vec3 diskColor = diskColorRamp(effTemp) * dopplerBoost * uDiskBrightness;
                
                // Alpha accumulation
                float alpha = clamp(plasmaDensity * 0.88, 0.0, 0.96);
                accumColor += (1.0 - accumAlpha) * diskColor * alpha;
                accumAlpha += (1.0 - accumAlpha) * alpha;
                
                if (accumAlpha >= 0.97) break;
            }
        }
        
        // Spacetime Curvature Grid visualization (embedded Flamm paraboloid funnel)
        if (uEnableGrid == 1) {
            // Check proximity to Flamm's paraboloid funnel: y_grid = -1.8 * sqrt(rs * max(0.0, r - rs))
            float rProj = length(rayPos.xz);
            if (rProj > rs) {
                float ySurface = -1.8 * sqrt(rs * (rProj - rs));
                float prevDist = prevPos.y - (-1.8 * sqrt(rs * max(0.0, length(prevPos.xz) - rs)));
                float nextDist = nextPos.y - ySurface;
                if (prevDist * nextDist <= 0.0 && rProj < 18.0 * rs) {
                    float angle = atan(rayPos.z, rayPos.x);
                    float radialLines = abs(fract(angle * 12.0 / PI) - 0.5);
                    float circleLines = abs(fract(rProj / (0.8 * rs)) - 0.5);
                    float gridLine = smoothstep(0.42, 0.49, max(1.0 - radialLines * 2.0, 1.0 - circleLines * 2.0));
                    vec3 gridColor = vec3(0.05, 0.65, 1.0) * 1.5;
                    accumColor += (1.0 - accumAlpha) * gridColor * gridLine * 0.8;
                    accumAlpha += (1.0 - accumAlpha) * gridLine * 0.5;
                }
            }
        }
        
        prevPos = rayPos;
        rayPos = nextPos;
        rayDir = nextDir;
    }
    
    // Photon sphere edge glow
    if (hitHorizon) {
        // Pure black inside event horizon shadow
        accumColor = accumColor * accumAlpha;
    } else {
        // Background skybox / stars
        if (uEnableStars == 1 && accumAlpha < 1.0) {
            vec3 v = rayDir;
            float starU = atan(v.z, v.x) / (2.0 * PI) + 0.5;
            float starV = asin(clamp(v.y, -1.0, 1.0)) / PI + 0.5;
            
            vec3 texStars = texture(uStarsTexture, vec2(starU, starV)).rgb;
            
            // Procedural pinpoint sharp stars
            vec3 p1 = floor(v * 160.0);
            float s1 = pow(max(0.0, hash3(p1) - 0.982), 2.0) * 1800.0;
            vec3 p2 = floor(v * 320.0);
            float s2 = pow(max(0.0, hash3(p2) - 0.990), 2.0) * 3200.0;
            
            // Cosmic interstellar nebula glow
            vec3 nebula = vec3(0.03, 0.05, 0.12) * (sin(v.x * 2.5 + v.y * 1.8) * 0.5 + 0.5);
            vec3 starField = texStars * 1.3 + vec3(s1) + vec3(s2 * 0.85, s2 * 0.95, s2) + nebula;
            
            accumColor += (1.0 - accumAlpha) * starField;
        }
    }
    
    // Subtle gravitational photon ring glow right around critical shadow boundary
    float impactB = sqrt(L2);
    float bCrit = 2.598 * rs;
    float ringDiff = abs(impactB - bCrit) / rs;
    if (ringDiff < 0.08 && !hitHorizon) {
        float photonRingGlow = exp(-ringDiff * 45.0) * 0.45;
        accumColor += vec3(1.0, 0.85, 0.6) * photonRingGlow;
    }
    
    // Tone mapping and gamma correction
    vec3 mapped = accumColor / (accumColor + vec3(1.0));
    mapped = pow(mapped, vec3(1.0 / 2.2));
    
    FragColor = vec4(mapped, 1.0);
}
