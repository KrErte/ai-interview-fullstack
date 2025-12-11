import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import {
  MergedDimensionScore,
  SoftSkillMergeRequest,
  SoftSkillMergeResponse,
  SoftSkillMergedProfile,
  SoftSkillMergerApiService
} from '../../core/services/soft-skill-merger-api.service';
import {
  SoftSkillCatalogService,
  SoftSkillDimension
} from '../../core/services/soft-skill-catalog.service';

type InterviewerStyle = 'HR' | 'TECHNICAL' | 'MIXED';

interface StylePreset {
  title: string;
  hint: string;
}

@Component({
  selector: 'app-soft-skill-merger-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './soft-skill-merger-page.component.html',
  styleUrls: ['./soft-skill-merger-page.component.scss']
})
export class SoftSkillMergerPageComponent implements OnInit {
  form: FormGroup;

  loading = false;
  errorMessage = '';
  successMessage = '';
  mergedProfile: SoftSkillMergedProfile | null = null;
  lastResponseMeta: Pick<
    SoftSkillMergeResponse,
    'createdAt' | 'saved' | 'savedProfileId'
  > | null = null;
  parsedRequest: SoftSkillMergeRequest | null = null;
  selectedStyle: InterviewerStyle = 'MIXED';
  dimensionsMap: Record<string, SoftSkillDimension> = {};
  expandedDimensions = new Set<string>();

  readonly stylePresets: Record<InterviewerStyle, StylePreset[]> = {
    HR: [
      {
        title: 'How does the candidate handle conflict in a team?',
        hint: 'Look for empathy, de-escalation, constructive feedback.'
      },
      {
        title: 'How does the candidate respond to feedback?',
        hint: 'Growth mindset, openness, specific examples.'
      },
      {
        title: 'What motivates this candidate in the long term?',
        hint: 'Intrinsic motivation, alignment with company values.'
      }
    ],
    TECHNICAL: [
      {
        title: 'How does the candidate debug complex production issues?',
        hint: 'Systematic approach, logging, monitoring.'
      },
      {
        title: 'Describe a time they had to choose between speed and quality.',
        hint: 'Trade-offs, risk management.'
      },
      {
        title: 'How do they explain technical topics to non-technical people?',
        hint: 'Clarity, analogies, patience.'
      }
    ],
    MIXED: [
      {
        title: 'What kind of environment helps this candidate perform at their best?',
        hint: 'Team culture, autonomy, support.'
      },
      {
        title: 'How do they react when a sprint goal is suddenly changed?',
        hint: 'Adaptability, communication.'
      },
      {
        title: 'How do they collaborate with QA/PM/Design?',
        hint: 'Cross-functional teamwork, ownership.'
      }
    ]
  };

  readonly sampleJson = `{
  "email": "candidate@example.com",
  "saveMerged": true,
  "sources": [
    {
      "sourceType": "TEAM_LEAD",
      "label": "Tech screen",
      "content": "Clear communicator, asks clarifying questions, strong ownership."
    },
    {
      "sourceType": "HR",
      "label": "Hiring manager",
      "content": "Collaborative, keeps stakeholders updated, good stress management."
    }
  ]
}`;

  constructor(
    private readonly fb: FormBuilder,
    private readonly mergerApi: SoftSkillMergerApiService,
    private readonly auth: AuthService,
    private readonly catalog: SoftSkillCatalogService
  ) {
    this.form = this.fb.group({
      jsonInput: ['', [Validators.required]],
      emailOverride: [this.auth.getCurrentUserEmail() || ''],
      saveMerged: [true],
      interviewerStyle: [this.selectedStyle]
    });
  }

  ngOnInit(): void {
    this.loadDimensions();
  }

  get currentStyle(): InterviewerStyle {
    const value = this.form.get('interviewerStyle')?.value as InterviewerStyle;
    return value || this.selectedStyle;
  }

  get currentPreset(): StylePreset[] {
    return this.stylePresets[this.currentStyle] || [];
  }

  get canSubmit(): boolean {
    return !this.loading && !!this.form.get('jsonInput')?.value;
  }

  onPasteSample(): void {
    this.form.get('jsonInput')?.setValue(this.sampleJson);
  }

  insertSkeleton(): void {
    const email =
      (this.form.get('emailOverride')?.value || '').toString().trim() ||
      this.auth.getCurrentUserEmail() ||
      'candidate@example.com';
    const style = this.currentStyle;
    this.selectedStyle = style;
    const sourceType =
      style === 'HR' ? 'HR' : style === 'TECHNICAL' ? 'TECH_LEAD' : 'MIXED';

    const skeleton = {
      email,
      saveMerged: true,
      sources: [
        {
          sourceType,
          label: 'Interview notes',
          content: 'Paste your interviewer notes here.'
        }
      ]
    };

    this.form.get('jsonInput')?.setValue(JSON.stringify(skeleton, null, 2));
  }

  merge(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.mergedProfile = null;
    this.lastResponseMeta = null;

    const request = this.buildRequestFromInput();
    if (!request) {
      return;
    }

    this.parsedRequest = request;
    this.loading = true;

    this.mergerApi
      .mergeSoftSkills(request)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: response => {
          this.mergedProfile = response.mergedProfile;
          this.lastResponseMeta = {
            createdAt: response.createdAt,
            saved: response.saved,
            savedProfileId: response.savedProfileId
          };
          this.successMessage = response.saved
            ? 'Merged profile created and saved.'
            : 'Merged profile created (not persisted).';
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.error('Merge request failed', err);
          this.errorMessage =
            err?.error?.message ||
            'Unable to merge soft skills right now. Please check the JSON and try again.';
        }
      });
  }

  private buildRequestFromInput(): SoftSkillMergeRequest | null {
    const rawJson = (this.form.get('jsonInput')?.value || '').toString().trim();
    const emailOverride = (this.form.get('emailOverride')?.value || '')
      .toString()
      .trim();
    const saveMergedFlag = !!this.form.get('saveMerged')?.value;

    if (!rawJson) {
      this.errorMessage = 'Paste the AI JSON response before merging.';
      return null;
    }

    let parsed: any;
    try {
      parsed = JSON.parse(rawJson);
    } catch (err: any) {
      this.errorMessage = `Invalid JSON: ${
        err?.message || 'could not parse input.'
      }`;
      return null;
    }

    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      this.errorMessage = 'Expected a JSON object with email and sources.';
      return null;
    }

    const email =
      emailOverride ||
      (parsed.email && parsed.email.toString().trim()) ||
      '';
    if (!email) {
      this.errorMessage =
        'Email is required. Provide it in the JSON under "email" or via the override field.';
      return null;
    }

    const sourcesRaw: any[] = Array.isArray(parsed.sources)
      ? parsed.sources
      : [];
    const sources = sourcesRaw
      .map(src => ({
        sourceType: (src?.sourceType || 'OTHER').toString().trim(),
        label: src?.label ? src.label.toString().trim() : null,
        content: (src?.content || '').toString().trim(),
        dimensionScores: Array.isArray(src?.dimensionScores)
          ? src.dimensionScores.map((d: any) => ({
              dimension: (d?.dimension || '').toString().trim(),
              score:
                d?.score === undefined || d?.score === null
                  ? null
                  : Number(d.score),
              explanation: d?.explanation
                ? d.explanation.toString().trim()
                : null
            }))
          : undefined
      }))
      .filter(s => !!s.content);

    if (!sources.length) {
      this.errorMessage =
        'At least one source with "sourceType" and non-empty "content" is required.';
      return null;
    }

    const request: SoftSkillMergeRequest = {
      email,
      jobContext: parsed.jobContext ? parsed.jobContext.toString().trim() : null,
      sources,
      saveMerged: parsed.saveMerged ?? saveMergedFlag
    };

    return request;
  }

  confidencePct(confidence: number | null | undefined): number {
    if (
      confidence === null ||
      confidence === undefined ||
      Number.isNaN(confidence)
    ) {
      return 0;
    }
    const pct = Math.round(Math.min(Math.max(confidence, 0), 1) * 100);
    return pct;
  }

  confidenceTone(
    confidence: number | null | undefined
  ): 'low' | 'medium' | 'high' {
    const value = confidence ?? 0;
    if (value >= 0.67) return 'high';
    if (value >= 0.34) return 'medium';
    return 'low';
  }

  trackByDimension(_index: number, dim: MergedDimensionScore): string {
    return dim?.dimension || String(_index);
  }

  dimensionMeta(key: string): SoftSkillDimension | null {
    return this.dimensionsMap[key] || null;
  }

  humanLabel(key: string): string {
    return this.dimensionsMap[key]?.label || key;
  }

  toggleDim(key: string): void {
    if (this.expandedDimensions.has(key)) {
      this.expandedDimensions.delete(key);
    } else {
      this.expandedDimensions.add(key);
    }
    this.expandedDimensions = new Set(this.expandedDimensions);
  }

  isExpanded(key: string): boolean {
    return this.expandedDimensions.has(key);
  }

  private loadDimensions(): void {
    this.catalog.getDimensions().subscribe({
      next: (dims) => {
        this.dimensionsMap = dims.reduce((acc, dim) => {
          acc[dim.key] = dim;
          return acc;
        }, {} as Record<string, SoftSkillDimension>);
      },
      error: () => {
        this.dimensionsMap = {};
      }
    });
  }
}