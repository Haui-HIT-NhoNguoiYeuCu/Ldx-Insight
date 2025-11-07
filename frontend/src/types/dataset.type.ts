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

export type DatasetRequestParams = {
  q?: string;
  category?: string;
  page?: number;
  size?: number;
  sort?: string;
};

export type DatasetResponse = {
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  content: any[];
  first: boolean;
  last: boolean;
};
