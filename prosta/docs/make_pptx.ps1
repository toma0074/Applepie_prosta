param([string]$Json, [string]$Out)

function RGBc($r, $g, $b) { return [int]($r + $g * 256 + $b * 65536) }

$NAVY   = RGBc 15 42 92
$BLUE   = RGBc 27 74 156
$YELLOW = RGBc 245 215 110
$PALE   = RGBc 232 240 252
$BG     = RGBc 247 249 252
$GRAY   = RGBc 74 85 104
$WHITE  = RGBc 255 255 255
$FONT   = "Meiryo UI"
$DOT = [string][char]0x25CF + " "

$slides = Get-Content -Raw -Encoding UTF8 $Json | ConvertFrom-Json
$app = New-Object -ComObject PowerPoint.Application
$pres = $app.Presentations.Add(-1)
$pres.PageSetup.SlideWidth = 960
$pres.PageSetup.SlideHeight = 540
$layout = $pres.SlideMaster.CustomLayouts.Item(7)

function Add-Text($slide, $text, $x, $y, $w, $h, $size, $color, $bold, $align) {
  $s = $slide.Shapes.AddTextbox(1, $x, $y, $w, $h)
  $r = $s.TextFrame.TextRange
  $r.Text = $text
  $r.Font.Name = $FONT
  $r.Font.Size = $size
  $r.Font.Color.RGB = $color
  $r.Font.Bold = [int]$bold
  $r.ParagraphFormat.Alignment = [int]$align
  $s.TextFrame.WordWrap = -1
  return $s
}

function Add-Box($slide, $shapeType, $x, $y, $w, $h, $fill, $line) {
  $s = $slide.Shapes.AddShape($shapeType, $x, $y, $w, $h)
  $s.Fill.ForeColor.RGB = $fill
  if ([int]$line -ge 0) { $s.Line.ForeColor.RGB = [int]$line; $s.Line.Weight = 1.5 } else { $s.Line.Visible = 0 }
  $s.Shadow.Visible = 0
  return $s
}

function Set-ShapeText($s, $text, $size, $color, $bold) {
  $r = $s.TextFrame.TextRange
  $r.Text = $text
  $r.Font.Name = $FONT
  $r.Font.Size = $size
  $r.Font.Bold = [int]$bold
  $r.Font.Color.RGB = $color
}

function Add-Cards($slide, $cards, $top, $height) {
  $n = $cards.Count
  $gap = 24
  $w = (880 - $gap * ($n - 1)) / $n
  for ($i = 0; $i -lt $n; $i++) {
    $x = 40 + $i * ($w + $gap)
    $box = Add-Box $slide 5 $x $top $w $height $WHITE $NAVY
    $box.Adjustments.Item(1) = 0.06
    $head = Add-Box $slide 5 $x $top $w 44 $NAVY -1
    $head.Adjustments.Item(1) = 0.3
    Set-ShapeText $head $cards[$i].head 20 $WHITE -1
    $text = ($cards[$i].items | ForEach-Object { $DOT + $_ }) -join "`r"
    $t = Add-Text $slide $text ($x + 14) ($top + 56) ($w - 28) ($height - 66) 18 $GRAY 0 1
    $t.TextFrame.TextRange.ParagraphFormat.SpaceAfter = 8
  }
}

$idx = 0
foreach ($d in $slides) {
  $idx++
  $slide = $pres.Slides.AddSlide($idx, $layout)
  while ($slide.Shapes.Count -gt 0) { $slide.Shapes.Item(1).Delete() }
  $slide.FollowMasterBackground = 0
  $slide.Background.Fill.Solid()
  $slide.Background.Fill.ForeColor.RGB = $BG

  if ($d.type -eq "title" -or $d.type -eq "closing") {
    $slide.Background.Fill.ForeColor.RGB = $NAVY
    Add-Box $slide 9 700 90 200 250 $YELLOW -1 | Out-Null
    Add-Box $slide 9 750 190 22 30 $NAVY -1 | Out-Null
    Add-Box $slide 9 818 190 22 30 $NAVY -1 | Out-Null
    $sm = Add-Box $slide 1 775 250 40 6 $NAVY -1
  }

  if ($d.type -eq "title") {
    Add-Box $slide 1 60 195 8 150 $YELLOW -1 | Out-Null
    Add-Text $slide $d.title 90 185 560 90 60 $WHITE -1 1 | Out-Null
    Add-Text $slide $d.subtitle 90 285 600 50 24 $YELLOW 0 1 | Out-Null
    Add-Text $slide $d.footer 90 440 560 30 16 $WHITE 0 1 | Out-Null
  }
  elseif ($d.type -eq "closing") {
    Add-Text $slide $d.title 50 40 620 50 32 $WHITE -1 1 | Out-Null
    $b = Add-Text $slide (($d.bullets | ForEach-Object { $DOT + $_ }) -join "`r") 50 120 620 160 22 $WHITE 0 1
    $b.TextFrame.TextRange.ParagraphFormat.SpaceAfter = 10
    $m = Add-Box $slide 5 50 320 620 70 $YELLOW -1
    Set-ShapeText $m $d.message 28 $NAVY -1
    Add-Text $slide $d.footer 50 440 620 30 18 $WHITE 0 1 | Out-Null
  }
  else {
    Add-Box $slide 1 0 0 960 76 $NAVY -1 | Out-Null
    Add-Box $slide 1 0 76 960 5 $YELLOW -1 | Out-Null
    Add-Text $slide $d.title 40 14 640 50 30 $WHITE -1 1 | Out-Null
    if ($d.time) {
      $tg = Add-Box $slide 5 740 22 180 32 $BLUE -1
      $tg.Adjustments.Item(1) = 0.5
      Set-ShapeText $tg $d.time 14 $WHITE 0
    }

    if ($d.type -eq "cards") {
      Add-Cards $slide $d.cards 120 330
    }
    elseif ($d.type -eq "flow") {
      $n = $d.steps.Count
      $w = 180; $gap = (880 - $w * $n) / ($n - 1)
      for ($i = 0; $i -lt $n; $i++) {
        $x = 40 + $i * ($w + $gap)
        $st = Add-Box $slide 5 $x 105 $w 50 $BLUE -1
        $st.Adjustments.Item(1) = 0.5
        Set-ShapeText $st $d.steps[$i] 18 $WHITE -1
        if ($i -lt $n - 1) {
          Add-Text $slide ([string][char]0x25B6) ($x + $w + 2) 115 ($gap - 4) 30 16 $NAVY 0 2 | Out-Null
        }
      }
      Add-Cards $slide $d.cards 185 300
    }
    else {
      $hasImg = [bool]$d.image
      $bw = 880; if ($hasImg) { $bw = 440 }
      $b = Add-Text $slide (($d.bullets | ForEach-Object { $DOT + $_ }) -join "`r") 40 110 $bw 330 20 $GRAY 0 1
      $b.TextFrame.TextRange.ParagraphFormat.SpaceAfter = 14
      if ($hasImg) {
        $ph = Add-Box $slide 5 510 110 410 300 $PALE $BLUE
        $ph.Adjustments.Item(1) = 0.04
        $ph.Line.DashStyle = 4
        Set-ShapeText $ph $d.image 16 $BLUE 0
      }
    }

    if ($d.demo) {
      $dm = Add-Box $slide 5 40 450 880 44 $YELLOW -1
      $dm.Adjustments.Item(1) = 0.3
      Set-ShapeText $dm ([string][char]0x25B6 + " " + $d.demo) 18 $NAVY -1
    }
    Add-Text $slide "PROSTA" 820 505 120 24 12 $BLUE -1 3 | Out-Null
  }

  if ($d.notes) { $slide.NotesPage.Shapes.Placeholders.Item(2).TextFrame.TextRange.Text = $d.notes }
}

$pres.SaveAs($Out)
$pres.Close()
$app.Quit()
"saved: $Out"
