// types/dataset.ts
export type Dataset = {
  id: number;
  name: string;
  category:
    | 'Agriculture'
    | 'Health'
    | 'Finance'
    | 'Education'
    | 'Transportation'
    | 'Environment';
  description: string;
  downloads: number;
  views: number;
  updated: string; // ISO date
  format: string; //VD: "CSV, JSON"
};
