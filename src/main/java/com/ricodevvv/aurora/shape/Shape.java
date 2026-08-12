package com.ricodevvv.aurora.shape;

import com.ricodevvv.aurora.util.VectorMath;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Una figura es simplemente una lista de offsets relativos al origen.
 * Se calcula una vez y se reusa; las transformaciones devuelven copias.
 */
public interface Shape {

    List<Vector> points();

    static Shape of(List<Vector> points) {
        return () -> points;
    }

    default Shape rotateY(double radians) {
        return transform(v -> VectorMath.rotateY(v, radians));
    }

    default Shape rotateX(double radians) {
        return transform(v -> VectorMath.rotateX(v, radians));
    }

    default Shape rotateZ(double radians) {
        return transform(v -> VectorMath.rotateZ(v, radians));
    }

    default Shape scale(double factor) {
        return transform(v -> v.multiply(factor));
    }

    default Shape translate(double x, double y, double z) {
        return transform(v -> v.add(new Vector(x, y, z)));
    }

    /** Orienta la figura hacia donde mira la location (util para escudos/impactos). */
    default Shape facing(Location location) {
        return transform(v -> VectorMath.rotateToDirection(v, location));
    }

    default Shape transform(java.util.function.UnaryOperator<Vector> op) {
        List<Vector> copy = new ArrayList<>(points().size());
        for (Vector v : points()) copy.add(op.apply(v.clone()));
        return of(copy);
    }
}
