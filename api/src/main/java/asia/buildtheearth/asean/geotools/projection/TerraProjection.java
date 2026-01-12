/*
 *    Adapted from GeoTools - The Open Source Java GIS Toolkit
 *    https://geotools.org
 *
 *    This file includes refactored and modified portions of the original
 *    org.geotools.referencing.operation.projection.MapProjection class.
 *
 *    Original authors include André Gosselin, Martin Desruisseaux, and Rueben Schulz.
 *
 *    Licensed under the GNU Lesser General Public License (LGPL), version 2.1 or later.
 */
package asia.buildtheearth.asean.geotools.projection;

import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import org.geotools.api.referencing.operation.MathTransform2D;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.referencing.operation.projection.ProjectionException;
import org.geotools.referencing.operation.transform.AbstractMathTransform;
import org.jetbrains.annotations.NotNull;

import java.awt.geom.Point2D;
import java.io.Serial;
import java.io.Serializable;

/**
 * Base class for custom map projections based on the BuildTheEarth project's
 * {@linkplain GeographicProjection Terra Projection}.
 *
 * <p>This transform strips down most of the internal map calculations used in traditional projections
 * and delegates transformation logic to a defined {@link GeographicProjection} instance.
 * Currently, only {@link GeographicProjection#fromGeo(double, double)} and
 * {@link GeographicProjection#toGeo(double, double)} are used for forward and inverse transformations.</p>
 *
 * <p>Large portions of code and documentation were adapted from:<br>
 * GeoTools {@link org.geotools.referencing.operation.projection.MapProjection}</p>
 *
 * <p><strong>Note:</strong> Parameter descriptors and projection metadata are not implemented in this class.</p>
 *
 * <h2>Attribution</h2>
 * <p>Original GeoTools authors:</p>
 * <ul>
 *     <li>André Gosselin</li>
 *     <li>Martin Desruisseaux (PMO, IRD)</li>
 *     <li>Rueben Schulz</li>
 * </ul>
 *
 * <sub>Licensed under the GNU Lesser General Public License (LGPL) v2.1 or later.</sub>
 *
 * @deprecated See {@link DymaxionMapProjection}
 */
@Deprecated
public class TerraProjection extends AbstractMathTransform implements MathTransform2D, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The projection of this class
     */
    private final GeographicProjection projection;

    /**
     * Inverse projection of this {@link #projection}
     */
    private transient MathTransform2D inverse;

    /**
     * Construct a new projection from BuildTheEarth project defined projection.
     *
     * @param projection The projection definition as {@link GeographicProjection}
     */
    public TerraProjection(GeographicProjection projection) {
        this.projection = projection;
    }

    /** Returns the dimension of input points. */
    @Override
    public final int getSourceDimensions() {
        return 2;
    }

    /** Returns the dimension of output points. */
    @Override
    public final int getTargetDimensions() {
        return 2;
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                          ////////
    ////////                          TRANSFORMATION METHODS                          ////////
    ////////             Includes an inner class for inverse projections.             ////////
    ////////                                                                          ////////
    //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
     *
     * <p>This transforms the input coordinate into projected planar coordinates using the
     * defined {@link GeographicProjection#fromGeo(double, double)} method.</p>
     *
     * @param ptSrc the specified coordinate point to be transformed. Ordinates must be in decimal degrees.
     * @param ptDst the specified coordinate point that stores the result of transforming {@code ptSrc}, or
     *     {@code null}. Ordinates will be in metres.
     * @return the coordinate point after transforming {@code ptSrc} and storing the result in {@code ptDst}.
     * @throws ProjectionException if the point can't be transformed.
     *
     * @see GeographicProjection#fromGeo(double, double)
     */
    @Override
    public final Point2D transform(@NotNull Point2D ptSrc, Point2D ptDst) throws ProjectionException {
        double x = ptSrc.getX();
        double y = ptSrc.getY();

        try {
            double[] projected = this.projection.fromGeo(x, y);

            if (ptDst == null) ptDst = new Point2D.Double();
            ptDst.setLocation(projected[0], projected[1]);
        }
        catch (OutOfProjectionBoundsException | ArrayIndexOutOfBoundsException ex) {
            throw new ProjectionException(ex);
        }

        return ptDst;
    }

    /**
     * Transforms a list of coordinate point ordinal values. Ordinates must be
     * (<var>longitude</var>,<var>latitude</var>) pairs in decimal degrees.
     *
     * @throws ProjectionException if a point can't be transformed. This method tries to transform every points even if
     *     some of them can't be transformed. Non-transformable points will have value {@link Double#NaN}. If more than
     *     one point can't be transformed, then this exception may be about an arbitrary point.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
        /*
         * Vérifie s'il faudra parcourir le tableau en sens inverse.
         * Ce sera le cas si les tableaux source et destination se
         * chevauchent et que la destination est après la source.
         */
        final boolean reverse = (srcPts == dstPts && srcOff < dstOff && srcOff + (2 * numPts) > dstOff);
        if (reverse) {
            srcOff += 2 * numPts;
            dstOff += 2 * numPts;
        }
        final Point2D.Double point = new Point2D.Double();
        ProjectionException firstException = null;
        while (--numPts >= 0) {
            try {
                point.x = srcPts[srcOff++];
                point.y = srcPts[srcOff++];
                transform(point, point);
                dstPts[dstOff++] = point.x;
                dstPts[dstOff++] = point.y;
            } catch (ProjectionException exception) {
                dstPts[dstOff++] = Double.NaN;
                dstPts[dstOff++] = Double.NaN;
                if (firstException == null) {
                    firstException = exception;
                }
            }
            if (reverse) {
                srcOff -= 4;
                dstOff -= 4;
            }
        }
        if (firstException != null) {
            throw firstException;
        }
    }

    /**
     * Transforms a list of coordinate point ordinal values. Ordinates must be
     * (<var>longitude</var>,<var>latitude</var>) pairs in decimal degrees.
     *
     * @throws ProjectionException if a point can't be transformed. This method tries to transform every points even if
     *     some of them can't be transformed. Non-transformable points will have value {@link Float#NaN}. If more than
     *     one point can't be transformed, then this exception may be about an arbitrary point.
     */
    @Override
    public final void transform(float[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) throws ProjectionException {
        boolean reverse = srcPts == dstPts && srcOff < dstOff && srcOff + 2 * numPts > dstOff;
        if (reverse) {
            srcOff += 2 * numPts;
            dstOff += 2 * numPts;
        }

        Point2D.Double point = new Point2D.Double();
        ProjectionException firstException = null;

        while(true) {
            --numPts;
            if (numPts < 0) {
                if (firstException != null) {
                    throw firstException;
                } else {
                    return;
                }
            }

            try {
                point.x = (double) srcPts[srcOff++];
                point.y = (double) srcPts[srcOff++];
                this.transform(point, point);
                dstPts[dstOff++] = (float) point.x;
                dstPts[dstOff++] = (float) point.y;
            } catch (ProjectionException ex) {
                dstPts[dstOff++] = Float.NaN;
                dstPts[dstOff++] = Float.NaN;
                if (firstException == null) {
                    firstException = ex;
                }
            }

            if (reverse) {
                srcOff -= 4;
                dstOff -= 4;
            }
        }
    }

    /** Returns the inverse of this map projection. */
    @Override
    public final MathTransform2D inverse() {
        // No synchronization. Not a big deal if this method is invoked in
        // the same time by two threads resulting in two instances created.
        if (inverse == null) {
            inverse = new TerraProjection.Inverse();
        }
        return inverse;
    }

    /**
     * Inverse of a map projection.
     *
     * <p>Will be created by {@link TerraProjection#inverse()} only when first required.
     * Implementation of {@code transform(...)} methods are mostly identical to {@code GeoProjectionTransform.transform(...)},
     * except that they will invokes {@link GeographicProjection#toGeo(double, double)} instead of
     * {@link GeographicProjection#fromGeo(double, double)}.</p>
     *
     * Original authors:
     * <ul>
     *     <li>Martin Desruisseaux (PMO, IRD)</li>
     * </ul>
     *
     * @see GeographicProjection#toGeo(double, double)
     */
    private final class Inverse extends AbstractMathTransform.Inverse implements MathTransform2D {
        @Serial
        private static final long serialVersionUID = 1L;

        /** Default constructor. */
        public Inverse() {
            TerraProjection.this.super();
        }

        /**
         * Inverse transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
         *
         *
         * @param ptSrc the specified coordinate point to be transformed. Ordinates must be in metres.
         * @param ptDst the specified coordinate point that stores the result of transforming {@code ptSrc}, or
         *     {@code null}. Ordinates will be in decimal degrees.
         * @return the coordinate point after transforming {@code ptSrc} and stroring the result in {@code ptDst}.
         * @throws ProjectionException if the point can't be transformed.
         */
        public Point2D transform(@NotNull Point2D ptSrc, Point2D ptDst) throws ProjectionException {
            double x = ptSrc.getX();
            double y = ptSrc.getY();

            try {
                double[] projected = TerraProjection.this.projection.toGeo(x, y);

                if (ptDst == null) ptDst = new Point2D.Double();
                ptDst.setLocation(projected[0], projected[1]);
            }
            catch (OutOfProjectionBoundsException | ArrayIndexOutOfBoundsException ex) {
                throw new ProjectionException(ex);
            }

            return ptDst;
        }

        /**
         * Inverse transforms a list of coordinate point ordinal values. Ordinates must be (<var>x</var>,<var>y</var>)
         * pairs in metres.
         *
         * @throws ProjectionException if a point can't be transformed. This method tries to transform every points even
         *     if some of them can't be transformed. Non-transformable points will have value {@link Double#NaN}. If
         *     more than one point can't be transformed, then this exception may be about an arbitrary point.
         */
        public void transform(double[] src, int srcOffset, double[] dest, int dstOffset, int numPts) throws TransformException {
            /*
             * Vérifie s'il faudra parcourir le tableau en sens inverse.
             * Ce sera le cas si les tableaux source et destination se
             * chevauchent et que la destination est après la source.
             */
            final boolean reverse = (src == dest && srcOffset < dstOffset && srcOffset + (2 * numPts) > dstOffset);
            if (reverse) {
                srcOffset += 2 * numPts;
                dstOffset += 2 * numPts;
            }
            final Point2D.Double point = new Point2D.Double();
            ProjectionException firstException = null;
            while (--numPts >= 0) {
                try {
                    point.x = src[srcOffset++];
                    point.y = src[srcOffset++];
                    transform(point, point);
                    dest[dstOffset++] = point.x;
                    dest[dstOffset++] = point.y;
                } catch (ProjectionException exception) {
                    dest[dstOffset++] = Double.NaN;
                    dest[dstOffset++] = Double.NaN;
                    if (firstException == null) {
                        firstException = exception;
                    }
                }
                if (reverse) {
                    srcOffset -= 4;
                    dstOffset -= 4;
                }
            }
            if (firstException != null) {
                throw firstException;
            }
        }

        /**
         * Inverse transforms a list of coordinate point ordinal values. Ordinates must be (<var>x</var>,<var>y</var>)
         * pairs in metres.
         *
         * @throws ProjectionException if a point can't be transformed. This method tries to transform every points even
         *     if some of them can't be transformed. Non-transformable points will have value {@link Float#NaN}. If more
         *     than one point can't be transformed, then this exception may be about an arbitrary point.
         */
        public void transform(float[] src, int srcOffset, float[] dest, int dstOffset, int numPts) throws ProjectionException {
            final boolean reverse = (src == dest && srcOffset < dstOffset && srcOffset + (2 * numPts) > dstOffset);
            if (reverse) {
                srcOffset += 2 * numPts;
                dstOffset += 2 * numPts;
            }
            final Point2D.Double point = new Point2D.Double();
            ProjectionException firstException = null;
            while (--numPts >= 0) {
                try {
                    point.x = src[srcOffset++];
                    point.y = src[srcOffset++];
                    transform(point, point);
                    dest[dstOffset++] = (float) point.x;
                    dest[dstOffset++] = (float) point.y;
                } catch (ProjectionException exception) {
                    dest[dstOffset++] = Float.NaN;
                    dest[dstOffset++] = Float.NaN;
                    if (firstException == null) {
                        firstException = exception;
                    }
                }
                if (reverse) {
                    srcOffset -= 4;
                    dstOffset -= 4;
                }
            }
            if (firstException != null) {
                throw firstException;
            }
        }

        /** Returns the original map projection. */
        @Override
        public MathTransform2D inverse() {
            return (MathTransform2D) super.inverse();
        }
    }
}