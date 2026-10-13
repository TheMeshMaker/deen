import SwiftUI

/// Eight-pointed stars (two squares turned 45°) on staggered rows, as on Android. Small and spaced
/// apart: in a tight grid the gaps between stars form cross shapes, which Deen avoids.
struct GeometricPattern: View {
    var color: Color
    var cell: CGFloat = 44
    var lineWidth: CGFloat = 1
    /// Slides the tiling sideways (0..cell).
    var drift: CGFloat = 0

    var body: some View {
        Canvas { context, size in
            let star = Self.octagram(radius: cell * 0.28, rotation: 0)
            var row = 0
            var y: CGFloat = 0
            while y < size.height + cell {
                var x = (row % 2 == 0 ? 0 : cell / 2) - drift.truncatingRemainder(dividingBy: cell)
                while x < size.width + cell {
                    context.stroke(star.offsetBy(dx: x, dy: y), with: .color(color), lineWidth: lineWidth)
                    x += cell
                }
                y += cell * 0.75
                row += 1
            }
        }
        .accessibilityHidden(true)
    }

    /// The star's outline (16 vertices), centred on the origin.
    static func octagram(radius: CGFloat, rotation: Double) -> Path {
        var path = Path()
        for i in 0..<16 {
            let angle = (Double(i) * 22.5 - 90 + rotation) * .pi / 180
            let r = i % 2 == 0 ? radius : radius * 0.7654
            let point = CGPoint(x: r * cos(angle), y: r * sin(angle))
            if i == 0 { path.move(to: point) } else { path.addLine(to: point) }
        }
        path.closeSubpath()
        return path
    }
}

/// The pattern, drifting slowly unless the user asked for less motion.
struct DriftingPattern: View {
    var color: Color
    var cell: CGFloat = 44
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        TimelineView(.animation(minimumInterval: 1 / 30, paused: reduceMotion)) { context in
            let t = context.date.timeIntervalSinceReferenceDate
            GeometricPattern(color: color, cell: cell, drift: reduceMotion ? 0 : CGFloat(t.truncatingRemainder(dividingBy: 30) / 30) * cell)
        }
    }
}

/// Deen's mark, as on the app icon: the letter nun (ن), its bowl a crescent and its diamond dot a
/// star, inside a thin eight-pointed star. Same geometry as tools/icon/build_icon.py (66-unit box).
struct DeenMark: Shape {
    func path(in rect: CGRect) -> Path {
        let scale = min(rect.width, rect.height) / 66
        // Icon coordinates (108 box, centre 54) moved into the 66 box (offset 21).
        func p(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: rect.minX + (x - 21) * scale, y: rect.minY + (y - 21) * scale) }
        func circle(_ cx: CGFloat, _ cy: CGFloat, _ r: CGFloat) -> Path {
            let c = p(cx, cy)
            return Path(ellipseIn: CGRect(x: c.x - r * scale, y: c.y - r * scale, width: 2 * r * scale, height: 2 * r * scale))
        }
        let outerRadius: CGFloat = 31
        let innerRadius = outerRadius - 1.7 / sin(67.5 * .pi / 180)
        let center = p(54, 54)
        let ring = GeometricPattern.octagram(radius: outerRadius * scale, rotation: 22.5)
            .offsetBy(dx: center.x, dy: center.y)
            .subtracting(GeometricPattern.octagram(radius: innerRadius * scale, rotation: 22.5).offsetBy(dx: center.x, dy: center.y))
        let bowl = circle(54, 56, 15.5).subtracting(circle(55.2, 50.9, 13.9))
        var dot = Path()
        let d = p(54.8, 46.4)
        dot.move(to: CGPoint(x: d.x, y: d.y - 4.2 * scale))
        dot.addLine(to: CGPoint(x: d.x + 3.6 * scale, y: d.y))
        dot.addLine(to: CGPoint(x: d.x, y: d.y + 4.2 * scale))
        dot.addLine(to: CGPoint(x: d.x - 3.6 * scale, y: d.y))
        dot.closeSubpath()
        return ring.union(bowl).union(dot)
    }
}

extension ShapeStyle where Self == LinearGradient {
    /// The mark's gold, light at the top.
    static var deenGold: LinearGradient {
        LinearGradient(colors: [Color(argb: 0xFFFFF4D6), Color(argb: 0xFFE3B858)], startPoint: .top, endPoint: .bottom)
    }
}

/// Elapsed time towards the next prayer: a track, the arc, and a glowing tip.
struct ProgressRing: View {
    var progress: Double
    var color: Color

    private let line: CGFloat = 6

    var body: some View {
        GeometryReader { geo in
            let size = min(geo.size.width, geo.size.height)
            let p = min(max(progress, 0), 1)
            // The track, the arc and the dot all sit on the same circle: the middle of the line.
            let radius = (size - line) / 2
            let angle = 2 * Double.pi * p - Double.pi / 2
            ZStack {
                Circle().inset(by: line / 2).stroke(color.opacity(0.2), lineWidth: line)
                Circle()
                    .inset(by: line / 2)
                    .trim(from: 0, to: p)
                    .stroke(AngularGradient(colors: [color.opacity(0.35), color], center: .center, startAngle: .degrees(0),
                                            endAngle: .degrees(max(360 * p, 1))),
                            style: StrokeStyle(lineWidth: line, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                Circle()
                    .fill(color)
                    .frame(width: 12, height: 12)
                    .shadow(color: color.opacity(0.8), radius: 6)
                    .position(x: size / 2 + radius * cos(angle), y: size / 2 + radius * sin(angle))
            }
            .frame(width: size, height: size)
            // A clock runs clockwise in every language; without this, Arabic and Urdu mirror the dot's
            // position but not the arc, and the dot lands on the wrong side.
            .environment(\.layoutDirection, .leftToRight)
            .position(x: geo.size.width / 2, y: geo.size.height / 2)
        }
        .accessibilityHidden(true)
    }
}
