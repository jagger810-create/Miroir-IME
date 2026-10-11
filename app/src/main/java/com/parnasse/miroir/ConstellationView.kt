package com.parnasse.miroir

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

/**
 * ═══ LA GRAVURE (11/10/2026) — le mode graphe du Miroir ═══
 *
 * La projection lisible d'une note : le fil du bloc en bas (les nœuds posés
 * dans l'ordre de l'écriture, le centre au milieu), les voisins par embedding
 * en orbite au-dessus, les arêtes typées — fil (trait plein épais), temps
 * (pointillé, l'épaisseur porte le poids), sens (trait fin vers l'orbite).
 *
 * EPD : statique, jamais d'animation — les paliers (GC) sont le métier de
 * l'Activity. Pas d'anti-aliasing, pas d'alpha : l'épaisseur et le pointillé
 * portent le poids. Le tap sur un nœud est la navigation.
 */
class ConstellationView(context: Context) : View(context) {

    private class NoeudDessin(
        val n: ConstellationNoeud,
        var cx: Float = 0f,
        var cy: Float = 0f,
        val rect: RectF = RectF(),
    )

    private var centre: ConstellationNoeud? = null
    private var noeuds: List<NoeudDessin> = emptyList()
    private var voisins: List<NoeudDessin> = emptyList()
    private var aretes: List<ConstellationArete> = emptyList()

    /** Tap sur un nœud — l'Activity ouvre la page (la fenêtre reprend la main). */
    var onNodeTap: ((ConstellationNoeud) -> Unit)? = null

    private val filPaint = Paint().apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 4f; isAntiAlias = false
    }
    private val tempsPaint = Paint().apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = false
        pathEffect = DashPathEffect(floatArrayOf(8f, 7f), 0f)
    }
    private val sensPaint = Paint().apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.5f; isAntiAlias = false
    }
    private val solPaint = Paint().apply {
        color = Color.DKGRAY; style = Paint.Style.STROKE; strokeWidth = 1.5f; isAntiAlias = false
    }
    private val pageFillPaint = Paint().apply {
        color = Color.WHITE; style = Paint.Style.FILL; isAntiAlias = false
    }
    private val pageBordPaint = Paint().apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2.5f; isAntiAlias = false
    }
    private val centreFillPaint = Paint().apply {
        color = Color.BLACK; style = Paint.Style.FILL; isAntiAlias = false
    }
    private val voisinFillPaint = Paint().apply {
        color = Color.DKGRAY; style = Paint.Style.FILL; isAntiAlias = false
    }
    private val numeroPaint = Paint().apply {
        color = Color.DKGRAY; textSize = 20f; isAntiAlias = false; textAlign = Paint.Align.CENTER
    }
    private val titrePaint = Paint().apply {
        color = Color.BLACK; textSize = 28f; isAntiAlias = false; textAlign = Paint.Align.CENTER
    }
    private val etiqPaint = Paint().apply {
        color = Color.BLACK; textSize = 20f; isAntiAlias = false; textAlign = Paint.Align.CENTER
    }
    private val centreNumPaint = Paint().apply {
        color = Color.WHITE; textSize = 22f; isAntiAlias = false; textAlign = Paint.Align.CENTER
    }

    init {
        setBackgroundColor(Color.WHITE)
    }

    fun setConstellation(data: ConstellationData) {
        centre = data.centre
        aretes = data.aretes
        noeuds = data.noeuds.map { NoeudDessin(it) }
        voisins = data.voisins.map { NoeudDessin(it) }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = centre ?: return
        val W = width.toFloat()
        val H = height.toFloat()
        if (W < 20f || H < 20f) return

        // ═══ LES POSITIONS (calculées avant tout trait — les arêtes en boivent) ═══
        val yFil = H * 0.72f
        val n = noeuds.size
        var iCentre = noeuds.indexOfFirst { it.n.noteId == c.noteId }
        if (iCentre < 0) iCentre = n / 2
        val espac = if (n <= 1) 0f else (W * 0.85f / (n - 1)).coerceIn(30f, 110f)
        val centreX = W / 2f
        noeuds.forEachIndexed { i, nd ->
            nd.cx = centreX + (i - iCentre) * espac
            nd.cy = yFil
            nd.rect.set(nd.cx - 27f, nd.cy - 25f, nd.cx + 27f, nd.cy + 25f)
        }
        val rayon = (H * 0.30f).coerceAtLeast(110f)
        val m = voisins.size
        voisins.forEachIndexed { i, nd ->
            val ang = if (m <= 1) Math.PI / 2 else Math.PI * (i + 1) / (m + 1)
            nd.cx = centreX + (rayon * Math.cos(ang)).toFloat()
            nd.cy = yFil - (rayon * Math.sin(ang)).toFloat() * 0.85f
            nd.rect.set(nd.cx - 26f, nd.cy - 26f, nd.cx + 26f, nd.cy + 26f)
        }

        // ═══ LES ARÊTES (au fond) ═══
        // sens — le centre vers l'orbite, l'épaisseur porte le poids
        for (a in aretes) {
            if (a.type != "sens") continue
            val v = voisins.firstOrNull { it.n.noteId == a.vers } ?: continue
            sensPaint.strokeWidth = 1f + 3.5f * a.poids.coerceIn(0.0, 1.0).toFloat()
            canvas.drawLine(centreX, yFil, v.cx, v.cy, sensPaint)
        }
        // temps — la rafale : pointillé entre nœuds du fil
        for (a in aretes) {
            if (a.type != "temps") continue
            val nd = noeuds.firstOrNull { it.n.noteId == a.vers } ?: continue
            tempsPaint.strokeWidth = 1.5f + 2.5f * a.poids.coerceIn(0.0, 1.0).toFloat()
            canvas.drawLine(centreX, yFil, nd.cx, yFil, tempsPaint)
        }
        // fil — plein, épais, par-dessus
        for (a in aretes) {
            if (a.type != "fil") continue
            val nd = noeuds.firstOrNull { it.n.noteId == a.vers } ?: continue
            canvas.drawLine(centreX, yFil, nd.cx, yFil, filPaint)
        }

        // ═══ LE SOL DU FIL ═══
        val xMin = noeuds.minOfOrNull { it.cx } ?: centreX
        val xMax = noeuds.maxOfOrNull { it.cx } ?: centreX
        canvas.drawLine(xMin - 40f, yFil + 44f, xMax + 40f, yFil + 44f, solPaint)

        // ═══ LES VOISINS (l'orbite) ═══
        for (nd in voisins) {
            canvas.drawCircle(nd.cx, nd.cy, 19f, voisinFillPaint)
            val label = nd.n.title.ifEmpty { nd.n.preview }.take(14)
            canvas.drawText(label, nd.cx, nd.cy + 42f, etiqPaint)
        }

        // ═══ LES NŒUDS DU FIL (par-dessus les arêtes) ═══
        for (nd in noeuds) {
            val estCentre = nd.n.noteId == c.noteId
            if (estCentre) {
                canvas.drawRoundRect(nd.rect, 6f, 6f, centreFillPaint)
                if (nd.n.pageNumber >= 0) {
                    canvas.drawText("${nd.n.pageNumber}", nd.cx, nd.cy + 8f, centreNumPaint)
                }
            } else {
                canvas.drawRoundRect(nd.rect, 6f, 6f, pageFillPaint)
                canvas.drawRoundRect(nd.rect, 6f, 6f, pageBordPaint)
                if (nd.n.pageNumber >= 0) {
                    canvas.drawText("${nd.n.pageNumber}", nd.cx, nd.cy + 8f, numeroPaint)
                }
            }
        }

        // ═══ LE TITRE DU CENTRE ═══
        val titre = c.title.ifEmpty { c.preview }.take(44)
        canvas.drawText(titre, centreX, yFil + 82f, titrePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            val x = event.x
            val y = event.y
            for (nd in noeuds) {
                if (nd.rect.contains(x, y)) {
                    onNodeTap?.invoke(nd.n)
                    return true
                }
            }
            for (nd in voisins) {
                if (nd.rect.contains(x, y)) {
                    onNodeTap?.invoke(nd.n)
                    return true
                }
            }
        }
        return true
    }
}
