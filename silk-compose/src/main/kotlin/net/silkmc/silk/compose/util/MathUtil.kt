package net.silkmc.silk.compose.util

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.Vec3
import org.jetbrains.kotlinx.multik.api.linalg.dot
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.operations.minus
import org.jetbrains.kotlinx.multik.ndarray.operations.times

internal object MathUtil {

    fun Vec3.toMkArray() = mk.ndarray(doubleArrayOf(x, y, z))
    fun Vec3i.toMkArray() = mk.ndarray(doubleArrayOf(x.toDouble(), y.toDouble(), z.toDouble()))

    // taken from http://rosettacode.org/wiki/Find_the_intersection_of_a_line_with_a_plane#Kotlin
    fun rayPlaneIntersection(
        rayPoint: D1Array<Double>,
        rayVector: D1Array<Double>,
        planePoint: D1Array<Double>,
        planeNormal: D1Array<Double>,
    ): D1Array<Double> {
        val diff = rayPoint - planePoint
        val prod1 = diff dot planeNormal
        val prod2 = rayVector dot planeNormal
        val prod3 = prod1 / prod2
        return rayPoint - rayVector * prod3
    }

    fun BlockPos.withoutAxis(axis: Direction.Axis) = when (axis) {
        Direction.Axis.X -> z.toDouble() to y.toDouble()
        Direction.Axis.Z -> x.toDouble() to y.toDouble()
        // just for the compiler
        Direction.Axis.Y -> x.toDouble() to z.toDouble()
    }

    fun D1Array<Double>.withoutAxis(axis: Direction.Axis) = when (axis) {
        Direction.Axis.X -> this[2] to this[1]
        Direction.Axis.Z -> this[0] to this[1]
        // just for the compiler
        Direction.Axis.Y -> this[0] to this[2]
    }
}
