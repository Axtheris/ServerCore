package net.axther.serverCore.particle;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public enum EmitterPattern {

    // --- Basic shapes ---

    POINT {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            for (int i = 0; i < density; i++) {
                offsets.add(new Vector(
                        rng.nextDouble(-0.15, 0.15),
                        rng.nextDouble(-0.15, 0.15),
                        rng.nextDouble(-0.15, 0.15)
                ));
            }
            return offsets;
        }
    },

    RING {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            double rotation = tick * 0.1;
            for (int i = 0; i < density; i++) {
                double angle = rotation + (2 * Math.PI * i / density);
                offsets.add(new Vector(Math.cos(angle) * radius, 0, Math.sin(angle) * radius));
            }
            return offsets;
        }
    },

    COLUMN {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            for (int i = 0; i < density; i++) {
                double y = height * i / Math.max(density - 1, 1);
                offsets.add(new Vector(0, y, 0));
            }
            return offsets;
        }
    },

    // --- Classic animations ---

    HELIX {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density * 2);
            double progress = (tick % 60) / 60.0;
            for (int i = 0; i < density; i++) {
                double t = (progress + (double) i / density) % 1.0;
                double angle = t * 4 * Math.PI;
                double y = t * height;
                offsets.add(new Vector(Math.cos(angle) * radius, y, Math.sin(angle) * radius));
                offsets.add(new Vector(Math.cos(angle + Math.PI) * radius, y, Math.sin(angle + Math.PI) * radius));
            }
            return offsets;
        }
    },

    SPIRAL {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            double rotation = tick * 0.15;
            for (int i = 0; i < density; i++) {
                double t = (double) i / density;
                double r = t * radius;
                double angle = rotation + t * 4 * Math.PI;
                offsets.add(new Vector(Math.cos(angle) * r, 0, Math.sin(angle) * r));
            }
            return offsets;
        }
    },

    FOUNTAIN {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            for (int i = 0; i < density; i++) {
                double t = rng.nextDouble();
                double angle = rng.nextDouble(2 * Math.PI);
                double spread = t * radius;
                double y = height * t * (1 - t) * 4;
                offsets.add(new Vector(Math.cos(angle) * spread, y, Math.sin(angle) * spread));
            }
            return offsets;
        }
    },

    PULSE {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            double progress = (tick % 40) / 40.0;
            double currentRadius = progress * radius;
            for (int i = 0; i < density; i++) {
                double theta = Math.acos(1 - 2.0 * i / density);
                double phi = Math.PI * (1 + Math.sqrt(5)) * i;
                offsets.add(new Vector(
                        Math.sin(theta) * Math.cos(phi) * currentRadius,
                        Math.sin(theta) * Math.sin(phi) * currentRadius,
                        Math.cos(theta) * currentRadius
                ));
            }
            return offsets;
        }
    },

    // --- Heart & symbol shapes ---

    HEART {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            double beat = 1.0 + 0.15 * Math.sin(tick * 0.3); // heartbeat pulse
            for (int i = 0; i < density; i++) {
                double t = 2 * Math.PI * i / density;
                // parametric heart curve
                double x = 16 * Math.pow(Math.sin(t), 3);
                double y = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);
                offsets.add(new Vector(
                        x / 16.0 * radius * beat,
                        y / 16.0 * radius * beat,
                        0
                ));
            }
            // slowly rotate around Y axis
            double rot = tick * 0.05;
            double cosR = Math.cos(rot);
            double sinR = Math.sin(rot);
            for (Vector v : offsets) {
                double ox = v.getX();
                double oz = v.getZ();
                v.setX(ox * cosR - oz * sinR);
                v.setZ(ox * sinR + oz * cosR);
            }
            return offsets;
        }
    },

    STAR {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            double rotation = tick * 0.08;
            int points = 5;
            double innerRadius = radius * 0.4;
            int perSegment = Math.max(density / (points * 2), 1);
            for (int p = 0; p < points; p++) {
                double outerAngle = rotation + (2 * Math.PI * p / points) - Math.PI / 2;
                double innerAngle = rotation + (2 * Math.PI * (p + 0.5) / points) - Math.PI / 2;
                double ox = Math.cos(outerAngle) * radius;
                double oy = Math.sin(outerAngle) * radius;
                double ix = Math.cos(innerAngle) * innerRadius;
                double iy = Math.sin(innerAngle) * innerRadius;
                for (int j = 0; j < perSegment; j++) {
                    double t = (double) j / perSegment;
                    offsets.add(new Vector(ox + (ix - ox) * t, oy + (iy - oy) * t, 0));
                }
                double nextOuterAngle = rotation + (2 * Math.PI * ((p + 1) % points) / points) - Math.PI / 2;
                double nox = Math.cos(nextOuterAngle) * radius;
                double noy = Math.sin(nextOuterAngle) * radius;
                for (int j = 0; j < perSegment; j++) {
                    double t = (double) j / perSegment;
                    offsets.add(new Vector(ix + (nox - ix) * t, iy + (noy - iy) * t, 0));
                }
            }
            return offsets;
        }
    },

    INFINITY {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            double rotation = tick * 0.06;
            for (int i = 0; i < density; i++) {
                double t = rotation + 2 * Math.PI * i / density;
                // lemniscate of Bernoulli
                double denom = 1 + Math.sin(t) * Math.sin(t);
                double x = Math.cos(t) / denom * radius;
                double y = Math.sin(t) * Math.cos(t) / denom * radius;
                offsets.add(new Vector(x, y, 0));
            }
            return offsets;
        }
    },

    // --- 3D complex shapes ---

    SPHERE {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            double rotY = tick * 0.04;
            // Fibonacci sphere distribution
            for (int i = 0; i < density; i++) {
                double theta = Math.acos(1 - 2.0 * (i + 0.5) / density);
                double phi = Math.PI * (1 + Math.sqrt(5)) * i + rotY;
                offsets.add(new Vector(
                        Math.sin(theta) * Math.cos(phi) * radius,
                        Math.cos(theta) * radius,
                        Math.sin(theta) * Math.sin(phi) * radius
                ));
            }
            return offsets;
        }
    },

    CUBE {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            double rotY = tick * 0.05;
            double rotX = tick * 0.03;
            double cosY = Math.cos(rotY), sinY = Math.sin(rotY);
            double cosX = Math.cos(rotX), sinX = Math.sin(rotX);
            int perEdge = Math.max(density / 12, 2);
            // 12 edges of a cube
            double[][] corners = {
                {-1,-1,-1},{1,-1,-1},{1,1,-1},{-1,1,-1},
                {-1,-1,1},{1,-1,1},{1,1,1},{-1,1,1}
            };
            int[][] edges = {
                {0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}
            };
            for (int[] edge : edges) {
                double[] a = corners[edge[0]], b = corners[edge[1]];
                for (int j = 0; j < perEdge; j++) {
                    double t = (double) j / perEdge;
                    double x = (a[0] + (b[0] - a[0]) * t) * radius * 0.5;
                    double y = (a[1] + (b[1] - a[1]) * t) * radius * 0.5;
                    double z = (a[2] + (b[2] - a[2]) * t) * radius * 0.5;
                    // rotate Y then X
                    double rx = x * cosY + z * sinY;
                    double rz = -x * sinY + z * cosY;
                    double ry = y * cosX - rz * sinX;
                    double rz2 = y * sinX + rz * cosX;
                    offsets.add(new Vector(rx, ry, rz2));
                }
            }
            return offsets;
        }
    },

    TORNADO {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            double rotation = tick * 0.2;
            for (int i = 0; i < density; i++) {
                double t = (double) i / density;
                double y = t * height;
                double r = t * radius; // widens as it goes up
                double angle = rotation + t * 8 * Math.PI; // many revolutions
                double jitter = ThreadLocalRandom.current().nextDouble(-0.1, 0.1);
                offsets.add(new Vector(Math.cos(angle) * (r + jitter), y, Math.sin(angle) * (r + jitter)));
            }
            return offsets;
        }
    },

    DNA {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density * 2);
            double progress = tick * 0.08;
            int rungs = density / 3;
            // two helical backbones
            for (int i = 0; i < density; i++) {
                double t = (double) i / density;
                double y = t * height;
                double angle = progress + t * 6 * Math.PI;
                offsets.add(new Vector(Math.cos(angle) * radius, y, Math.sin(angle) * radius));
                offsets.add(new Vector(Math.cos(angle + Math.PI) * radius, y, Math.sin(angle + Math.PI) * radius));
            }
            // cross-rungs connecting the two strands
            for (int i = 0; i < rungs; i++) {
                double t = (double) i / rungs;
                double y = t * height;
                double angle = progress + t * 6 * Math.PI;
                double x1 = Math.cos(angle) * radius;
                double z1 = Math.sin(angle) * radius;
                double x2 = Math.cos(angle + Math.PI) * radius;
                double z2 = Math.sin(angle + Math.PI) * radius;
                offsets.add(new Vector((x1 + x2) * 0.5, y, (z1 + z2) * 0.5));
            }
            return offsets;
        }
    },

    TORUS {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            double rotY = tick * 0.04;
            double tubeRadius = radius * 0.35;
            int rings = (int) Math.sqrt(density);
            int perRing = Math.max(density / rings, 1);
            for (int i = 0; i < rings; i++) {
                double theta = 2 * Math.PI * i / rings + rotY;
                for (int j = 0; j < perRing; j++) {
                    double phi = 2 * Math.PI * j / perRing;
                    double x = (radius + tubeRadius * Math.cos(phi)) * Math.cos(theta);
                    double z = (radius + tubeRadius * Math.cos(phi)) * Math.sin(theta);
                    double y = tubeRadius * Math.sin(phi);
                    offsets.add(new Vector(x, y, z));
                }
            }
            return offsets;
        }
    },

    // --- Dynamic / animated ---

    ORBIT {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density * 3);
            // 3 orbiting rings at different tilts
            for (int ring = 0; ring < 3; ring++) {
                double tilt = Math.PI * ring / 3;
                double speed = 0.1 + ring * 0.03;
                double rot = tick * speed;
                double cosTilt = Math.cos(tilt), sinTilt = Math.sin(tilt);
                int perRing = density;
                for (int i = 0; i < perRing; i++) {
                    double angle = rot + 2 * Math.PI * i / perRing;
                    double x = Math.cos(angle) * radius;
                    double y = Math.sin(angle) * radius;
                    // tilt the ring
                    offsets.add(new Vector(x, y * cosTilt, y * sinTilt));
                }
            }
            return offsets;
        }
    },

    WAVE {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            int side = (int) Math.ceil(Math.sqrt(density));
            for (int i = 0; i < side; i++) {
                for (int j = 0; j < side; j++) {
                    double x = (i - side / 2.0) / side * radius * 2;
                    double z = (j - side / 2.0) / side * radius * 2;
                    double dist = Math.sqrt(x * x + z * z);
                    double y = Math.sin(dist * 3 - tick * 0.15) * height * 0.3;
                    offsets.add(new Vector(x, y, z));
                    if (offsets.size() >= density) return offsets;
                }
            }
            return offsets;
        }
    },

    VORTEX {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            double rotation = tick * 0.25;
            for (int i = 0; i < density; i++) {
                double t = (double) i / density;
                double y = t * height;
                double r = radius * (1 - t * 0.7); // narrows toward top
                double angle = rotation + t * 10 * Math.PI;
                offsets.add(new Vector(Math.cos(angle) * r, y, Math.sin(angle) * r));
            }
            return offsets;
        }
    },

    RAIN {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            for (int i = 0; i < density; i++) {
                double x = rng.nextDouble(-radius, radius);
                double z = rng.nextDouble(-radius, radius);
                // drops fall over time, wrap around
                double phase = (tick * 0.15 + rng.nextDouble() * height) % height;
                double y = height - phase;
                offsets.add(new Vector(x, y, z));
            }
            return offsets;
        }
    },

    FIREWORK {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            // burst cycle: 60 ticks
            double progress = (tick % 60) / 60.0;
            double burstRadius = progress * radius;
            double gravity = progress * progress * height * 0.5; // gravity pull
            // Fibonacci sphere burst
            for (int i = 0; i < density; i++) {
                double theta = Math.acos(1 - 2.0 * (i + 0.5) / density);
                double phi = Math.PI * (1 + Math.sqrt(5)) * i;
                double x = Math.sin(theta) * Math.cos(phi) * burstRadius;
                double y = Math.cos(theta) * burstRadius + height * 0.5 - gravity;
                double z = Math.sin(theta) * Math.sin(phi) * burstRadius;
                offsets.add(new Vector(x, y, z));
            }
            return offsets;
        }
    },

    BUTTERFLY {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            double wingFlap = Math.sin(tick * 0.2) * 0.6 + 0.4; // wing flap animation
            double drift = tick * 0.03;
            for (int i = 0; i < density; i++) {
                double t = 2 * Math.PI * i / density;
                // butterfly curve (parametric)
                double r = Math.exp(Math.cos(t)) - 2 * Math.cos(4 * t) + Math.pow(Math.sin(t / 12.0), 5);
                double x = Math.sin(t) * r * radius * 0.25;
                double y = Math.cos(t) * r * radius * 0.25 * wingFlap;
                offsets.add(new Vector(x, y + Math.sin(drift) * 0.3, 0));
            }
            // rotate
            double rot = tick * 0.04;
            double cosR = Math.cos(rot), sinR = Math.sin(rot);
            for (Vector v : offsets) {
                double ox = v.getX(), oz = v.getZ();
                v.setX(ox * cosR - oz * sinR);
                v.setZ(ox * sinR + oz * cosR);
            }
            return offsets;
        }
    },

    GALAXY {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            double rotation = tick * 0.03;
            int arms = 3;
            for (int i = 0; i < density; i++) {
                int arm = i % arms;
                double armOffset = 2 * Math.PI * arm / arms;
                double t = rng.nextDouble();
                double r = t * radius;
                double angle = rotation + armOffset + t * 3.0; // spiral outward
                double scatter = rng.nextGaussian() * 0.12 * radius;
                double x = Math.cos(angle) * r + Math.cos(angle + Math.PI / 2) * scatter;
                double z = Math.sin(angle) * r + Math.sin(angle + Math.PI / 2) * scatter;
                double y = rng.nextGaussian() * 0.05 * radius; // thin disk
                offsets.add(new Vector(x, y, z));
            }
            return offsets;
        }
    },

    ATOM {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            // nucleus cluster
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            int nucleusCount = Math.max(density / 6, 2);
            for (int i = 0; i < nucleusCount; i++) {
                offsets.add(new Vector(
                        rng.nextGaussian() * 0.15,
                        rng.nextGaussian() * 0.15,
                        rng.nextGaussian() * 0.15
                ));
            }
            // 3 electron orbits at different tilts
            int electronsPerOrbit = (density - nucleusCount) / 3;
            double[][] tilts = {{0, 0}, {Math.PI / 3, 0}, {0, Math.PI / 3}};
            for (int orbit = 0; orbit < 3; orbit++) {
                double tiltX = tilts[orbit][0];
                double tiltZ = tilts[orbit][1];
                double speed = 0.12 + orbit * 0.04;
                double rot = tick * speed;
                for (int i = 0; i < electronsPerOrbit; i++) {
                    double angle = rot + 2 * Math.PI * i / electronsPerOrbit;
                    double x = Math.cos(angle) * radius;
                    double y = Math.sin(angle) * radius;
                    // apply tilt rotations
                    double y2 = y * Math.cos(tiltX) - 0 * Math.sin(tiltX);
                    double z2 = y * Math.sin(tiltX);
                    double x2 = x * Math.cos(tiltZ) + z2 * Math.sin(tiltZ);
                    double z3 = -x * Math.sin(tiltZ) + z2 * Math.cos(tiltZ);
                    offsets.add(new Vector(x2, y2, z3));
                }
            }
            return offsets;
        }
    },

    DIAMOND {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            double rotY = tick * 0.06;
            double cosR = Math.cos(rotY), sinR = Math.sin(rotY);
            int perEdge = Math.max(density / 8, 2);
            // diamond = two pyramids stacked, 4 top edges + 4 bottom edges
            double halfH = height * 0.5;
            double[][] topEdges = {{radius,0},{0,radius},{-radius,0},{0,-radius}};
            for (int e = 0; e < 4; e++) {
                double x1 = topEdges[e][0], z1 = topEdges[e][1];
                // top point to equator
                for (int j = 0; j < perEdge; j++) {
                    double t = (double) j / perEdge;
                    double x = x1 * t, z = z1 * t;
                    double y = halfH * (1 - t);
                    double rx = x * cosR - z * sinR;
                    double rz = x * sinR + z * cosR;
                    offsets.add(new Vector(rx, y, rz));
                }
                // equator to bottom point
                for (int j = 0; j < perEdge; j++) {
                    double t = (double) j / perEdge;
                    double x = x1 * (1 - t), z = z1 * (1 - t);
                    double y = -halfH * t;
                    double rx = x * cosR - z * sinR;
                    double rz = x * sinR + z * cosR;
                    offsets.add(new Vector(rx, y, rz));
                }
            }
            return offsets;
        }
    },

    DRAGON_BREATH {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            double breathProgress = (tick % 40) / 40.0;
            for (int i = 0; i < density; i++) {
                double t = rng.nextDouble() * breathProgress;
                double spread = t * radius * (0.5 + breathProgress);
                double angle = rng.nextDouble(2 * Math.PI);
                double x = Math.cos(angle) * spread * rng.nextDouble(0.6, 1.0);
                double z = Math.sin(angle) * spread * rng.nextDouble(0.6, 1.0);
                double y = t * height + rng.nextDouble(-0.2, 0.2);
                offsets.add(new Vector(x, y, z));
            }
            return offsets;
        }
    },

    WINGS {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            double flap = Math.sin(tick * 0.15) * 0.4; // gentle flap
            int half = density / 2;
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < half; i++) {
                    double t = (double) i / half;
                    // wing shape — wide at base, tapers to tip
                    double x = side * t * radius;
                    double wingHeight = Math.sin(t * Math.PI) * height * 0.6;
                    double y = wingHeight;
                    double z = flap * t * t * side; // flap bends wing tips
                    offsets.add(new Vector(x, y, z));
                }
            }
            return offsets;
        }
    },

    CLOCK {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            // clock face ring
            int ringCount = (int) (density * 0.6);
            for (int i = 0; i < ringCount; i++) {
                double angle = 2 * Math.PI * i / ringCount;
                offsets.add(new Vector(Math.cos(angle) * radius, Math.sin(angle) * radius, 0));
            }
            // 12 hour markers
            for (int h = 0; h < 12; h++) {
                double angle = 2 * Math.PI * h / 12 - Math.PI / 2;
                double markerR = radius * 0.85;
                offsets.add(new Vector(Math.cos(angle) * markerR, Math.sin(angle) * markerR, 0));
            }
            // hour hand
            double hourAngle = -(tick * 0.002) - Math.PI / 2;
            int handPoints = Math.max((density - ringCount - 12) / 2, 2);
            for (int i = 0; i < handPoints; i++) {
                double t = (double) i / handPoints * radius * 0.5;
                offsets.add(new Vector(Math.cos(hourAngle) * t, Math.sin(hourAngle) * t, 0));
            }
            // minute hand
            double minAngle = -(tick * 0.024) - Math.PI / 2;
            for (int i = 0; i < handPoints; i++) {
                double t = (double) i / handPoints * radius * 0.7;
                offsets.add(new Vector(Math.cos(minAngle) * t, Math.sin(minAngle) * t, 0));
            }
            return offsets;
        }
    },

    FLAME_JET {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            double flicker = 0.8 + 0.2 * Math.sin(tick * 0.4);
            for (int i = 0; i < density; i++) {
                double t = rng.nextDouble();
                double y = t * height * flicker;
                double spread = (1 - t) * radius * 0.3 + t * radius; // wider at top
                double angle = rng.nextDouble(2 * Math.PI);
                double wobble = Math.sin(tick * 0.3 + t * 5) * 0.15;
                offsets.add(new Vector(
                        Math.cos(angle) * spread * (1 - t * 0.5) + wobble,
                        y,
                        Math.sin(angle) * spread * (1 - t * 0.5) + wobble
                ));
            }
            return offsets;
        }
    },

    SNOWFALL {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            for (int i = 0; i < density; i++) {
                // each flake has a deterministic position based on its index
                double seed = i * 7919.0; // prime for distribution
                double x = Math.sin(seed) * radius;
                double z = Math.cos(seed * 1.3) * radius;
                double drift = Math.sin(tick * 0.05 + seed * 0.01) * 0.3;
                double phase = (tick * 0.06 + (seed % 100) / 100.0 * height) % height;
                double y = height - phase;
                offsets.add(new Vector(x + drift, y, z));
            }
            return offsets;
        }
    },

    PORTAL {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            double rotation = tick * 0.12;
            for (int i = 0; i < density; i++) {
                double t = (double) i / density;
                double angle = rotation + t * 6 * Math.PI;
                // particles spiral inward
                double r = radius * (1 - t * 0.8);
                double y = Math.sin(t * Math.PI) * height * 0.5;
                offsets.add(new Vector(Math.cos(angle) * r, y, Math.sin(angle) * r));
            }
            return offsets;
        }
    },

    METEOR {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            // meteor arcs across the sky cyclically
            double progress = (tick % 80) / 80.0;
            double meteorX = (progress - 0.5) * radius * 4;
            double meteorY = height - (progress - 0.5) * (progress - 0.5) * 4 * height;
            // head of meteor
            for (int i = 0; i < density / 3; i++) {
                offsets.add(new Vector(
                        meteorX + rng.nextGaussian() * 0.15,
                        meteorY + rng.nextGaussian() * 0.15,
                        rng.nextGaussian() * 0.15
                ));
            }
            // trail
            for (int i = 0; i < density - density / 3; i++) {
                double t = rng.nextDouble() * 0.4;
                double trailX = meteorX - t * radius * 2;
                double trailY = meteorY + t * height * 0.5;
                offsets.add(new Vector(
                        trailX + rng.nextGaussian() * 0.2 * (1 + t),
                        trailY + rng.nextGaussian() * 0.2 * (1 + t),
                        rng.nextGaussian() * 0.15 * (1 + t)
                ));
            }
            return offsets;
        }
    },

    MATRIX {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            int columns = (int) Math.ceil(Math.sqrt(density));
            for (int col = 0; col < columns; col++) {
                double x = (col - columns / 2.0) / columns * radius * 2;
                double z = 0;
                // each column has its own falling speed
                double speed = 0.1 + (col * 13 % 7) * 0.02;
                int perCol = Math.max(density / columns, 1);
                for (int j = 0; j < perCol; j++) {
                    double phase = (tick * speed + j * height / perCol) % (height * 1.5);
                    double y = height - phase;
                    if (y >= -0.5 && y <= height) {
                        double fade = 1 - (double) j / perCol; // leading particles brighter
                        offsets.add(new Vector(x, y, z));
                    }
                }
            }
            return offsets;
        }
    },

    CROWN {
        @Override
        public List<Vector> computeOffsets(double radius, double height, int tick, int density) {
            List<Vector> offsets = new ArrayList<>(density);
            double rotation = tick * 0.04;
            int spikes = 5;
            int perSpike = density / spikes;
            for (int s = 0; s < spikes; s++) {
                double baseAngle = rotation + 2 * Math.PI * s / spikes;
                double nextAngle = rotation + 2 * Math.PI * (s + 0.5) / spikes;
                for (int j = 0; j < perSpike; j++) {
                    double t = (double) j / perSpike;
                    if (t < 0.5) {
                        // going up to spike tip
                        double u = t * 2;
                        double angle = baseAngle + (nextAngle - baseAngle) * 0 ;
                        offsets.add(new Vector(
                                Math.cos(baseAngle) * radius,
                                u * height,
                                Math.sin(baseAngle) * radius
                        ));
                    } else {
                        // coming back down to valley
                        double u = (t - 0.5) * 2;
                        double angle = baseAngle + (nextAngle - baseAngle) * u;
                        offsets.add(new Vector(
                                Math.cos(angle) * radius,
                                height * (1 - u * 0.6),
                                Math.sin(angle) * radius
                        ));
                    }
                }
            }
            return offsets;
        }
    };

    public abstract List<Vector> computeOffsets(double radius, double height, int tick, int density);
}
