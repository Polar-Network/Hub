package net.polar.utils;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

public class Cuboid {

    private final Point topLeft;
    private final Point bottomRight;
    private final Point bottomLeft;
    private final Point topRight;

    public Cuboid(Point topLeft, Point bottomRight) {
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
        this.bottomLeft = new Vec(topLeft.x(), bottomRight.y(), topLeft.z());
        this.topRight = new Vec(bottomRight.x(), topLeft.y(), bottomRight.z());
    }

    public Cuboid(Point topLeft, Point bottomRight, Point bottomLeft, Point topRight) {
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
    }

    public boolean contains(Point point) {
        return Math.min(topLeft.x(), bottomRight.x()) <= point.x() && point.x() <= Math.max(topLeft.x(), bottomRight.x()) &&
                Math.min(topLeft.y(), bottomRight.y()) <= point.y() && point.y() <= Math.max(topLeft.y(), bottomRight.y()) &&
                Math.min(topLeft.z(), bottomRight.z()) <= point.z() && point.z() <= Math.max(topLeft.z(), bottomRight.z());
    }

    public Point center() {
        return new Vec((topLeft.x() + bottomRight.x()) / 2, (topLeft.y() + bottomRight.y()) / 2, (topLeft.z() + bottomRight.z()) / 2);
    }

    public Point topLeft() {
        return topLeft;
    }

    public Point bottomRight() {
        return bottomRight;
    }

    public Point bottomLeft() {
        return bottomLeft;
    }

    public Point topRight() {
        return topRight;
    }

    @Override
    public String toString() {
        return "Cuboid{" +
                "topLeft=" + topLeft +
                ", bottomRight=" + bottomRight +
                ", bottomLeft=" + bottomLeft +
                ", topRight=" + topRight +
                '}';
    }
}
